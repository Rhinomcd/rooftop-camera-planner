#!/usr/bin/env bash
# Verification helper for Rooftop Lens. Never sends input to the game: a human plays, this observes.
set -euo pipefail
REPO="$(cd "$(dirname "$0")/../../.." && pwd)"
RUN_DIR="${LENS_RUN_DIR:-/tmp/rooftop-lens-verify}"
PID_FILE="$RUN_DIR/client.pid"
LOG="$RUN_DIR/client.log"
RECORDINGS="$HOME/.runelite/rooftop-lens/recordings"
mkdir -p "$RUN_DIR/evidence"

client_pid() { [[ -f $PID_FILE ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null && cat "$PID_FILE"; }

case "${1:-}" in
  launch)
    if pid=$(client_pid); then echo "already running: pid $pid"; exit 0; fi
    [[ -f $HOME/.runelite/credentials.properties ]] || { echo "missing ~/.runelite/credentials.properties (see SKILL.md Launch)"; exit 1; }
    (cd "$REPO" && setsid nohup sh ./gradlew run --no-daemon </dev/null >"$LOG" 2>&1 &)
    for _ in $(seq 1 120); do
      for p in $(pgrep java || true); do
        if tr '\0' ' ' </proc/"$p"/cmdline | grep -q 'rooftoplens.RooftopLensPluginTest'; then echo "$p" >"$PID_FILE"; fi
      done
      grep -q 'RooftopLensPlugin is now running' "$LOG" 2>/dev/null && break
      grep -q 'BUILD FAILED' "$LOG" 2>/dev/null && { tail -40 "$LOG"; exit 1; }
      sleep 2
    done
    "$0" doctor ;;
  doctor)
    pid=$(client_pid) || { echo "FAIL: no client started by this skill ($PID_FILE)"; exit 1; }
    echo "client pid $pid"
    grep -q 'RooftopLensPlugin is now running' "$LOG" && echo "ok: Rooftop Lens running" || { echo "FAIL: plugin not running"; exit 1; }
    grep -o 'Rooftop Lens recorder writing .*' "$LOG" | tail -1 || echo "warn: recorder not started"
    grep -E 'ERROR.*rooftoplens|Exception.*rooftoplens' "$LOG" | tail -3 || true ;;
  laps)
    grep -E 'Lap (learned|dropped)' "$LOG" | sed 's/.*RooftopLensPlugin - //' ;;
  screenshot)
    client_pid >/dev/null || { echo "no client"; exit 1; }
    snaps="$HOME/.runelite/rooftop-lens/snaps"
    before=$(ls "$snaps" 2>/dev/null | wc -l)
    touch "$HOME/.runelite/rooftop-lens/snap-request"
    for _ in $(seq 1 20); do
      [[ $(ls "$snaps" 2>/dev/null | wc -l) -gt $before ]] && break
      sleep 0.5
    done
    shot=$(ls -t "$snaps"/*.png 2>/dev/null | head -1)
    [[ -n $shot && $(ls "$snaps" | wc -l) -gt $before ]] || { echo "no frame: the recorder snaps on a game tick, so the human must be logged in"; exit 1; }
    cp "$shot" "$RUN_DIR/evidence/" && echo "$RUN_DIR/evidence/$(basename "$shot")" ;;
  latest-recording)
    ls -t "$RECORDINGS"/*.jsonl | head -1 ;;
  replay)
    rec="${2:-$("$0" latest-recording)}"
    (cd "$REPO" && sh ./gradlew replay -Precording="$rec" --no-daemon -q) ;;
  promote)
    rec="$2"; name="$3"; laps="$4"; drops="$5"; note="${6:-Recorded in-game.}"
    dest="$REPO/src/test/resources/recordings/$name.jsonl"
    printf '{"t":"expect","laps":%s,"drops":%s,"note":"%s"}\n' "$laps" "$drops" "$note" >"$dest"
    cat "$rec" >>"$dest"
    echo "$dest" ;;
  stop)
    pid=$(client_pid) || { echo "nothing to stop"; exit 0; }
    kill "$pid" && rm -f "$PID_FILE" && echo "stopped $pid (evidence kept in $RUN_DIR/evidence)" ;;
  *)
    echo "usage: lens.sh launch|doctor|laps|screenshot|latest-recording|replay [file]|promote <file> <name> <laps> <drops> [note]|stop"; exit 2 ;;
esac
