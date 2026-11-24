#!/usr/bin/env bash
# Emulator smoke test for the launcher.
#   usage: smoke-test.sh <apk> <output-dir>
#
# Installs the APK, makes it the home app, then checks that:
#   - Home lands on it, and Back leaves it there
#   - "Phone" opens the default dialer
#   - "Messages" opens the default SMS app
#   - it never crashes
# Screenshots and logs are written to <output-dir>.
set -euo pipefail

APK=$1
OUT=$2
PKG=app.focuslauncher
HOME_ACTIVITY="$PKG/.HomeActivity"
mkdir -p "$OUT"

log() { echo "::group::$*"; }
endlog() { echo "::endgroup::"; }
fail() { echo "::error::$*"; adb logcat -d > "$OUT/logcat.txt" || true; exit 1; }

# Command output is captured before grepping: with pipefail, `adb ... | grep -q`
# can report failure when grep exits early and adb dies of SIGPIPE.
activities() { adb shell dumpsys activity activities; }

top_activity() {
  local dump
  dump=$(activities)
  grep -m1 -E 'topResumedActivity|mResumedActivity' <<< "$dump" || true
}

# Wait up to 20s for the top resumed activity to match a pattern.
wait_top() {
  local pattern=$1
  for _ in $(seq 1 40); do
    grep -q "$pattern" <<< "$(top_activity)" && return 0
    sleep 0.5
  done
  echo "Top activity: $(top_activity)"
  return 1
}

# Wait up to 20s for the package to have any activity in the task stack.
wait_started() {
  local pkg=$1
  for _ in $(seq 1 40); do
    grep -qE "ActivityRecord\{[^}]* $pkg/" <<< "$(activities)" && return 0
    sleep 0.5
  done
  return 1
}

# Tap the centre of the view with the given resource id.
tap_id() {
  local id=$1 xml node x1 y1 x2 y2
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null
  xml=$(adb shell cat /sdcard/ui.xml)
  node=$(grep -oE "resource-id=\"$id\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" <<< "$xml" || true)
  [ -n "$node" ] || fail "View $id not on screen"
  read -r x1 y1 x2 y2 <<< "$(grep -oE '[0-9]+' <<< "${node##*bounds=}" | tr '\n' ' ')"
  adb shell input tap $(((x1 + x2) / 2)) $(((y1 + y2) / 2))
}

go_home() {
  adb shell input keyevent KEYCODE_HOME
  wait_top "$HOME_ACTIVITY" || fail "Home did not return to the launcher"
}

log "Install and set as home"
adb install -r "$APK"
adb shell cmd package set-home-activity "$HOME_ACTIVITY"
adb logcat -c
endlog

log "Home"
go_home
sleep 1
adb exec-out screencap -p > "$OUT/home.png"
endlog

log "Back stays home"
adb shell input keyevent KEYCODE_BACK
sleep 1
wait_top "$HOME_ACTIVITY" || fail "Back left the launcher"
endlog

log "Phone opens the default dialer"
DIALER=$(adb shell telecom get-default-dialer | tr -d '\r' || true)
echo "Default dialer: ${DIALER:-<none>}"
tap_id "$PKG:id/phone"
if [ -n "$DIALER" ]; then
  wait_started "$DIALER" || fail "Tapping Phone did not open $DIALER"
fi
sleep 2
adb exec-out screencap -p > "$OUT/phone.png"
grep -q "$HOME_ACTIVITY" <<< "$(top_activity)" && fail "Tapping Phone did not leave the launcher"
go_home
endlog

log "Messages opens the default SMS app"
SMS=$(adb shell cmd role get-role-holders android.app.role.SMS | tr -d '\r' || true)
SMS=${SMS%%$'\n'*}
echo "Default SMS app: ${SMS:-<none>}"
tap_id "$PKG:id/messages"
if [ -n "$SMS" ]; then
  wait_started "$SMS" || fail "Tapping Messages did not open $SMS"
fi
sleep 2
adb exec-out screencap -p > "$OUT/messages.png"
grep -q "$HOME_ACTIVITY" <<< "$(top_activity)" && fail "Tapping Messages did not leave the launcher"
go_home
endlog

log "No crashes"
adb logcat -d > "$OUT/logcat.txt"
CRASHES=$(adb logcat -d -b crash)
if grep -q "Process: $PKG" <<< "$CRASHES"; then
  echo "$CRASHES"
  fail "The launcher crashed"
fi
endlog

echo "Smoke test passed"
