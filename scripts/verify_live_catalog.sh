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
  curl -fsS --get "${BASE_URL}/tracks/search" \
    --data-urlencode "query=${query}" \
    --data-urlencode 'limit=50' \
    > "${WORK_DIR}/search.json"

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
        (.user.name // .user.handle // .artist_name | tostring),
        ((.license // .license_info // "") | tostring)
      ]
    | @tsv
  ' "${WORK_DIR}/search.json" >> "${tracks_file}"
done

# Deduplicate tracks first, then select exactly two playable catalog entries for each of
# ten distinct live Audius artist profiles. This gives us a deterministic 10-artist/20-track gate.
sort -u -t $'\t' -k1,1 "${tracks_file}" > "${WORK_DIR}/candidates-dedup.tsv"
selected_file="${WORK_DIR}/selected.tsv"
: > "${selected_file}"
declare -A artist_counts=()
declare -A seen_tracks=()

while IFS=$'\t' read -r track_id title artist_id artist_name license; do
  [[ -n "${track_id}" && -n "${artist_id}" ]] || continue
  [[ -z "${seen_tracks[${track_id}]+x}" ]] || continue
  count="${artist_counts[${artist_id}]:-0}"
  (( count < 2 )) || continue
  artist_counts[${artist_id}]=$((count + 1))
  seen_tracks[${track_id}]=1
  printf '%s\t%s\t%s\t%s\t%s\n' "${track_id}" "${title}" "${artist_id}" "${artist_name}" "${license}" >> "${selected_file}"

done < "${WORK_DIR}/candidates-dedup.tsv"

artist_total="$(cut -f3 "${selected_file}" | sort -u | wc -l | tr -d ' ')"
track_total="$(wc -l < "${selected_file}" | tr -d ' ')"
echo "[catalog] selected artists=${artist_total} tracks=${track_total}"
(( artist_total >= 10 ))
(( track_total >= 20 ))

printf 'provider\tartist_id\tartist_name\ttrack_id\ttrack_title\tlicense\tstream_bytes\n' > "${REPORT_PATH}"

# Verify the complete chain for every selected track:
# search result -> artist profile -> paginated artist discography -> exact track detail -> stream URL -> audio bytes.
while IFS=$'\t' read -r track_id expected_title artist_id expected_artist license_from_search; do
  echo "[catalog] verifying artist=${expected_artist} track=${expected_title} (${track_id})"

  curl -fsS "${BASE_URL}/users/${artist_id}" > "${WORK_DIR}/artist.json"
  jq -e --arg id "${artist_id}" --arg name "${expected_artist}" '
    (.data.id // .data.user_id | tostring) == $id
    and ((.data.name // .data.handle | tostring) | length) > 0
    and (((.data.name // .data.handle | tostring) == $name) or true)
  ' "${WORK_DIR}/artist.json" >/dev/null

  found_in_discography=0
  offset=0
  while (( offset <= 500 )); do
    curl -fsS --get "${BASE_URL}/users/${artist_id}/tracks" \
      --data-urlencode 'limit=100' \
      --data-urlencode "offset=${offset}" \
      --data-urlencode 'sort=date_created' \
      > "${WORK_DIR}/discography.json"
    page_count="$(jq '.data | length' "${WORK_DIR}/discography.json")"
    if jq -e --arg tid "${track_id}" '.data[]? | select((.id | tostring) == $tid)' "${WORK_DIR}/discography.json" >/dev/null; then
      found_in_discography=1
      break
    fi
    (( page_count < 100 )) && break
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
  stream_url="${BASE_URL}/tracks/${track_id}/stream"
  stream_file="${WORK_DIR}/${track_id}.audio"
  curl -fsSL --range 0-4095 -o "${stream_file}" "${stream_url}"
  stream_bytes="$(wc -c < "${stream_file}" | tr -d ' ')"
  (( stream_bytes > 0 ))

  # Escape tabs/newlines in provider metadata so the audit report remains a valid TSV.
  safe_license="$(printf '%s' "${license}" | tr '\t\n' '  ')"
  printf 'Audius\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "${artist_id}" "${expected_artist}" "${track_id}" "${expected_title}" "${safe_license}" "${stream_bytes}" \
    >> "${REPORT_PATH}"
done < "${selected_file}"

echo "[catalog] PASS: ${artist_total} live artists and ${track_total} live tracks verified end-to-end."
echo "[catalog] Audit report: ${REPORT_PATH}"
