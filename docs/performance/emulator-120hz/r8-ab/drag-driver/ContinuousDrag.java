package org.week3.measure;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

import java.util.concurrent.locks.LockSupport;

/** External touch driver for the 1080 x 2424 test AVD. Runs in its own process. */
public final class ContinuousDrag extends Instrumentation {
    private static final long LEG_NS = 1_100_000_000L;
    private static final long SAMPLE_NS = 1_000_000_000L / 240;
    private int seconds;

    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        seconds = Integer.parseInt(args.getString("seconds", "30"));
        start();
    }

    private static void touch(UiAutomation ui, long downMs, int action, float y) {
        MotionEvent e = MotionEvent.obtain(downMs, SystemClock.uptimeMillis(), action, 540f, y, 0);
        try {
            e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            if (!ui.injectInputEvent(e, false)) throw new IllegalStateException("Input injection failed");
        } finally {
            e.recycle();
        }
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            UiAutomation ui = getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            long downMs = SystemClock.uptimeMillis();
            float y = 1850f;
            touch(ui, downMs, MotionEvent.ACTION_DOWN, y);
            try {
                long begin = SystemClock.elapsedRealtimeNanos();
                long finish = begin + seconds * 1_000_000_000L;
                int moves = 0;
                while (true) {
                    long now = SystemClock.elapsedRealtimeNanos();
                    if (now >= finish) break;
                    // Reverse direction every leg without lifting or restarting the input process.
                    double phase = ((now - begin) % (2 * LEG_NS)) / (double) LEG_NS;
                    y = 1850f - 600f * (float) Math.min(phase, 2.0 - phase);
                    touch(ui, downMs, MotionEvent.ACTION_MOVE, y);
                    moves++;
                    long after = SystemClock.elapsedRealtimeNanos();
                    long next = begin + ((after - begin) / SAMPLE_NS + 1) * SAMPLE_NS;
                    LockSupport.parkNanos(Math.min(next, finish) - after);
                }
                result.putLong("begin_ns", begin);
                result.putLong("end_ns", SystemClock.elapsedRealtimeNanos());
                result.putInt("moves", moves);
            } finally {
                touch(ui, downMs, MotionEvent.ACTION_UP, y);
            }
            finish(Activity.RESULT_OK, result);
        } catch (RuntimeException e) {
            result.putString("failure", e.toString());
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
