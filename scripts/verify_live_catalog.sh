#!/usr/bin/env bash
set -euo pipefail

BASE_URL="https://discoveryprovider.audius.co/v1"
WORK_DIR="${RUNNER_TEMP:-/tmp}/lanu-live-catalog"
REPORT_PATH="${GITHUB_WORKSPACE:-.}/build/live-catalog-verification.tsv"
mkdir -p "${WORK_DIR}" "$(dirname "${REPORT_PATH}")"
rm -f "${WORK_DIR}"/*

queries=(rock pop electronic "hip hop" acoustic jazz)
tracks_file="${WORK_DIR}/candidates.tsv"
: > "${tracks_file}"

for query in "${queries[@]}"; do
  echo "[catalog] searching Audius tracks: ${query}"
  safe_query="${query// /_}"
  curl -fsS --get "${BASE_URL}/tracks/search" \
    --data-urlencode "query=${query}" \
    --data-urlencode 'limit=50' \
    > "${WORK_DIR}/search-${safe_query}.json"

  jq -r '
    .data[]
    | select((.id // "") != "")
    | select((.title // .name // "") != "")
    | select((.user.id // .user.user_id // .artist_id // "") != "")
    | select((.user.name // .user.handle // .artist_name // "") != "")
    | select(((.is_streamable // .isStreamable // false) | tostring | ascii_downcase) == "true")
    | select(((.is_stream_gated // .isStreamGated // false) | tostring | ascii_downcase) != "true")
    | select(((.is_unlisted // .isUnlisted // false) | tostring | ascii_downcase) != "true")
    | [
        (.id | tostring),
        (.title // .name | tostring),
        (.user.id // .user.user_id // .artist_id | tostring),
        (.user.name // .user.handle // .artist_name | tostring)
      ]
    | @tsv
  ' "${WORK_DIR}/search-${safe_query}.json" >> "${tracks_file}"
done

sort -u -t $'\t' -k1,1 "${tracks_file}" > "${WORK_DIR}/candidates-dedup.tsv"
selected_file="${WORK_DIR}/selected.tsv"
: > "${selected_file}"

# A live search result can outlive its profile endpoint. Resolve the profile first,
# then use the provider-native handle for the documented handle-based track listing.
mapfile -t artist_ids < <(cut -f3 "${WORK_DIR}/candidates-dedup.tsv" | awk 'NF && !seen[$0]++')
validated_artists=0

for artist_id in "${artist_ids[@]}"; do
  (( validated_artists < 10 )) || break
  artist_candidates="${WORK_DIR}/artist-${artist_id}.tsv"
  awk -F '\t' -v id="${artist_id}" '$3 == id { print; if (++n == 2) exit }' "${WORK_DIR}/candidates-dedup.tsv" > "${artist_candidates}"
  [[ "$(wc -l < "${artist_candidates}")" -ge 2 ]] || continue

  if ! curl -fsS "${BASE_URL}/users/${artist_id}" > "${WORK_DIR}/artist-${artist_id}.json"; then
    echo "[catalog] skipping artist id=${artist_id}: profile endpoint rejected this result"
    continue
  fi

  handle="$(jq -r '(.data.handle // "") | tostring' "${WORK_DIR}/artist-${artist_id}.json")"
  artist_name="$(jq -r '(.data.name // .data.handle // "") | tostring' "${WORK_DIR}/artist-${artist_id}.json")"
  [[ -n "${handle}" && -n "${artist_name}" ]] || continue

  if ! curl -fsS --get "${BASE_URL}/users/handle/${handle}/tracks" \
      --data-urlencode 'limit=100' \
      --data-urlencode 'offset=0' \
      --data-urlencode 'sort=date_created' \
      > "${WORK_DIR}/discography-${artist_id}-0.json"; then
    echo "[catalog] skipping artist=${artist_name} id=${artist_id}: handle discography endpoint rejected this result"
    continue
  fi

  discography_count="$(jq '.data | length' "${WORK_DIR}/discography-${artist_id}-0.json")"
  (( discography_count > 0 )) || continue

  while IFS=$'\t' read -r track_id title _artist_id _artist_name; do
    printf '%s\t%s\t%s\t%s\t%s\n' "${track_id}" "${title}" "${artist_id}" "${artist_name}" "${handle}" >> "${selected_file}"
  done < "${artist_candidates}"
  validated_artists=$((validated_artists + 1))
done

artist_total="$(cut -f3 "${selected_file}" | sort -u | wc -l | tr -d ' ')"
track_total="$(wc -l < "${selected_file}" | tr -d ' ')"
echo "[catalog] selected validated artists=${artist_total} tracks=${track_total}"
(( artist_total >= 10 ))
(( track_total >= 20 ))

printf 'provider\tartist_id\tartist_name\ttrack_id\ttrack_title\tlicense\tstream_bytes\n' > "${REPORT_PATH}"

# Final end-to-end verification for every selected track:
# search result -> artist profile -> paginated artist discography -> exact track detail -> stream bytes.
while IFS=$'\t' read -r track_id expected_title artist_id expected_artist handle; do
  echo "[catalog] verifying artist=${expected_artist} track=${expected_title} (${track_id})"

  curl -fsS "${BASE_URL}/users/${artist_id}" > "${WORK_DIR}/artist.json"
  jq -e --arg id "${artist_id}" --arg handle "${handle}" '
    (.data.id // .data.user_id | tostring) == $id
    and ((.data.handle // "") | tostring) == $handle
    and ((.data.name // .data.handle // "") | tostring | length) > 0
  ' "${WORK_DIR}/artist.json" >/dev/null

  found_in_discography=0
  offset=0
  while (( offset <= 500 )); do
    if ! curl -fsS --get "${BASE_URL}/users/handle/${handle}/tracks" \
      --data-urlencode 'limit=100' \
      --data-urlencode "offset=${offset}" \
      --data-urlencode 'sort=date_created' \
      > "${WORK_DIR}/discography.json"; then
      echo "[catalog] FAIL: handle discography request rejected artist=${expected_artist} handle=${handle} offset=${offset}"
      exit 1
    fi
    page_count="$(jq '.data | length' "${WORK_DIR}/discography.json")"
    if jq -e --arg tid "${track_id}" '.data[]? | select((.id | tostring) == $tid)' "${WORK_DIR}/discography.json" >/dev/null; then
      found_in_discography=1
      break
    fi
    if (( page_count < 100 )); then break; fi
    offset=$((offset + 100))
  done
  (( found_in_discography == 1 ))

  curl -fsS "${BASE_URL}/tracks/${track_id}" > "${WORK_DIR}/track.json"
  jq -e \
    --arg tid "${track_id}" \
    --arg title "${expected_title}" \
    --arg artist_id "${artist_id}" \
    '.data
     | ((.id | tostring) == $tid)
     and ((.title // .name | tostring) == $title)
     and (((.user.id // .user.user_id // .artist_id) | tostring) == $artist_id)
     and (((.is_streamable // .isStreamable // false) | tostring | ascii_downcase) == "true")
     and (((.is_stream_gated // .isStreamGated // false) | tostring | ascii_downcase) != "true")
     and (((.is_unlisted // .isUnlisted // false) | tostring | ascii_downcase) != "true")
     and ((.id // "") != "")'
    "${WORK_DIR}/track.json" >/dev/null

  license="$(jq -r '(.data.license // .data.license_info // "") | tostring' "${WORK_DIR}/track.json")"
  stream_file="${WORK_DIR}/${track_id}.audio"
  curl -fsSL --range 0-4095 -o "${stream_file}" "${BASE_URL}/tracks/${track_id}/stream"
  stream_bytes="$(wc -c < "${stream_file}" | tr -d ' ')"
  (( stream_bytes > 0 ))

  safe_license="$(printf '%s' "${license}" | tr '\t\n' '  ')"
  printf 'Audius\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "${artist_id}" "${expected_artist}" "${track_id}" "${expected_title}" "${safe_license}" "${stream_bytes}" \
    >> "${REPORT_PATH}"
done < "${selected_file}"

echo "[catalog] PASS: ${artist_total} live artists and ${track_total} live tracks verified end-to-end."
echo "[catalog] Audit report: ${REPORT_PATH}"
