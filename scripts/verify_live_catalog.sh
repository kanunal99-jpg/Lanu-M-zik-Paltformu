#!/usr/bin/env bash
set -euo pipefail

BASE_URL="https://api.audius.co/v1"
STREAM_BASE_URL="https://discoveryprovider.audius.co/v1"
WORK_DIR="${RUNNER_TEMP:-/tmp}/lanu-live-catalog"
REPORT_PATH="${GITHUB_WORKSPACE:-.}/build/live-catalog-verification.tsv"
mkdir -p "${WORK_DIR}" "$(dirname "${REPORT_PATH}")"
rm -f "${WORK_DIR}"/*

request_json() {
  local url="$1"
  local output="$2"
  local attempt=1
  local http_code=""

  while (( attempt <= 4 )); do
    http_code="$(curl -sS -L --connect-timeout 8 --max-time 20 \
      -w '%{http_code}' -o "${output}" "${url}" || true)"
    if [[ "${http_code}" == "200" ]] && jq -e . "${output}" >/dev/null 2>&1; then
      sleep 0.15
      return 0
    fi
    if [[ "${http_code}" == "429" || "${http_code}" =~ ^5[0-9][0-9]$ ]]; then
      sleep $((attempt * 2))
    else
      sleep 1
    fi
    attempt=$((attempt + 1))
  done

  echo "[catalog] request failed: ${url}"
  echo "[catalog] response preview: $(head -c 180 "${output}" | tr '\n\r' '  ')"
  return 1
}

request_stream_bytes() {
  local url="$1"
  local output="$2"
  : > "${output}"

  # Some Audius gateways ignore HTTP Range and begin sending the full audio object.
  # Pipe into head so we capture only the first 4096 real audio bytes and intentionally
  # close the connection. curl may report a broken pipe after head exits; that is okay.
  set +e
  curl -fsSL --connect-timeout 8 --max-time 15 --retry 1 --retry-delay 1 \
    "${url}" | head -c 4096 > "${output}"
  local curl_status=${PIPESTATUS[0]}
  local head_status=${PIPESTATUS[1]}
  set -e

  local stream_bytes
  stream_bytes="$(wc -c < "${output}" | tr -d ' ')"
  echo "[catalog] stream probe: bytes=${stream_bytes}, curl_status=${curl_status}, head_status=${head_status}"
  (( stream_bytes > 0 ))
  sleep 0.15
}

encode() {
  python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1]))' "$1"
}

queries=(rock pop electronic "hip hop" acoustic jazz)
tracks_file="${WORK_DIR}/candidates.tsv"
: > "${tracks_file}"

for query in "${queries[@]}"; do
  echo "[catalog] searching Audius tracks: ${query}"
  safe_query="${query// /_}"
  request_json \
    "${BASE_URL}/tracks/search?query=$(encode "${query}")&limit=50" \
    "${WORK_DIR}/search-${safe_query}.json"
  jq -r '
    .data[]
    | select((.id // "") != "")
    | select((.title // .name // "") != "")
    | select((.user.id // .user.user_id // .artist_id // "") != "")
    | select((.user.name // .user.handle // .artist_name // "") != "")
    | select(((.is_streamable // .isStreamable // false) | tostring | ascii_downcase) == "true")
    | select(((.is_stream_gated // .isStreamGated // false) | tostring | ascii_downcase) != "true")
    | select(((.is_unlisted // .isUnlisted // false) | tostring | ascii_downcase) != "true")
    | [(.id|tostring),(.title // .name|tostring),(.user.id // .user.user_id // .artist_id|tostring),(.user.name // .user.handle // .artist_name|tostring)]
    | @tsv
  ' "${WORK_DIR}/search-${safe_query}.json" >> "${tracks_file}"
done

sort -u -t $'\t' -k1,1 "${tracks_file}" > "${WORK_DIR}/candidates-dedup.tsv"
selected_file="${WORK_DIR}/selected.tsv"
: > "${selected_file}"

# Phase 1: verify ten independent artist identities. Each artist must already have two
# distinct live search results, then its profile endpoint must resolve successfully.
mapfile -t artist_ids < <(cut -f3 "${WORK_DIR}/candidates-dedup.tsv" | awk 'NF && !seen[$0]++')
validated_artists=0

for artist_id in "${artist_ids[@]}"; do
  (( validated_artists < 10 )) || break
  artist_candidates="${WORK_DIR}/artist-${artist_id}.tsv"
  awk -F '\t' -v id="${artist_id}" '$3 == id { print; if (++n == 2) exit }' "${WORK_DIR}/candidates-dedup.tsv" > "${artist_candidates}"
  [[ "$(wc -l < "${artist_candidates}")" -ge 2 ]] || continue

  artist_json="${WORK_DIR}/artist-${artist_id}.json"
  request_json "${BASE_URL}/users/${artist_id}" "${artist_json}" || continue
  artist_name="$(jq -r '(.data.name // .data.handle // "") | tostring' "${artist_json}")"
  [[ -n "${artist_name}" ]] || continue

  while IFS=$'\t' read -r track_id title _artist_id _artist_name; do
    printf '%s\t%s\t%s\t%s\n' "${track_id}" "${title}" "${artist_id}" "${artist_name}" >> "${selected_file}"
  done < "${artist_candidates}"
  validated_artists=$((validated_artists + 1))
done

artist_total="$(cut -f3 "${selected_file}" | sort -u | wc -l | tr -d ' ')"
track_total="$(wc -l < "${selected_file}" | tr -d ' ')"
echo "[catalog] selected validated artists=${artist_total} tracks=${track_total}"
(( artist_total >= 10 ))
(( track_total >= 20 ))

printf 'provider\tartist_id\tartist_name\ttrack_id\ttrack_title\tlicense\tstream_bytes\n' > "${REPORT_PATH}"

# Phase 2: verify exact track identity/playability and actual audio bytes for all 20 tracks.
while IFS=$'\t' read -r track_id expected_title artist_id expected_artist; do
  echo "[catalog] verifying artist=${expected_artist} track=${expected_title} (${track_id})"

  track_json="${WORK_DIR}/track-${track_id}.json"
  request_json "${BASE_URL}/tracks/${track_id}" "${track_json}"
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
     and (((.is_unlisted // .isUnlisted // false) | tostring | ascii_downcase) != "true")' \
    "${track_json}" >/dev/null

  license="$(jq -r '(.data.license // .data.license_info // "") | tostring' "${track_json}")"
  stream_file="${WORK_DIR}/${track_id}.audio"
  request_stream_bytes "${STREAM_BASE_URL}/tracks/${track_id}/stream" "${stream_file}"
  stream_bytes="$(wc -c < "${stream_file}" | tr -d ' ')"
  (( stream_bytes > 0 ))

  safe_license="$(printf '%s' "${license}" | tr '\t\n' '  ')"
  printf 'Audius\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "${artist_id}" "${expected_artist}" "${track_id}" "${expected_title}" "${safe_license}" "${stream_bytes}" >> "${REPORT_PATH}"
done < "${selected_file}"

echo "[catalog] PASS: ${artist_total} live artists and ${track_total} live tracks verified end-to-end."
echo "[catalog] Audit report: ${REPORT_PATH}"