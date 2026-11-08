#!/system/bin/sh
# Run on the guest. Touch coordinates are obtained from the Compose accessibility tree.
set -eu
scenario=$1
seconds=$2
shift 2
edit_x=$1; edit_y=$2; all_x=$3; all_y=$4; garden_x=$5; garden_y=$6
salon_x=$7; salon_y=$8; lamp_x=$9; shift 9; lamp_y=$1

clock_cs() {
    read -r uptime unused < /proc/uptime
    echo "${uptime%.*}${uptime#*.}"
}
# /proc/uptime and Android FrameTimeline share the guest boot clock.
mark() { printf '%s %s\n' "$(clock_cs)" "$1"; }

n=0
mark BEGIN
deadline=$(( $(clock_cs) + seconds * 100 ))
while [ "$(clock_cs)" -lt "$deadline" ]; do
    case "$scenario" in
        scroll)
            mark "swipe $n begin"
            if [ $((n % 2)) -eq 0 ]; then
                input swipe 540 1850 540 1250 1100
            else
                input swipe 540 1250 540 1850 1100
            fi
            mark "swipe $n end"
            ;;
        search)
            input tap "$all_x" "$all_y"
            input tap "$edit_x" "$edit_y"
            input keycombination KEYCODE_CTRL_LEFT KEYCODE_A
            input keyevent KEYCODE_DEL
            sleep 0.12
            for letter in k a m e r a; do
                input text "$letter"
                sleep 0.06
            done
            input tap "$garden_x" "$garden_y"
            sleep 0.12
            input tap "$salon_x" "$salon_y"
            sleep 0.12
            ;;
        toggle)
            input tap "$lamp_x" "$lamp_y"
            sleep 0.18
            ;;
        *) exit 2 ;;
    esac
    n=$((n + 1))
done
# Odd parity gives the host an observable final switch state and Watt reading.
if [ "$scenario" = toggle ] && [ $((n % 2)) -eq 0 ]; then
    input tap "$lamp_x" "$lamp_y"
    n=$((n + 1))
fi
sleep 0.25
mark END
echo "$n"
