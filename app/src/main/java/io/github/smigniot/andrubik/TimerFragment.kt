package io.github.smigniot.andrubik

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.github.smigniot.andrubik.databinding.FragmentTimerBinding
import java.util.Locale

/**
 * Speedcubing timer with the standard frictionless interaction:
 *
 *   IDLE     -- touch down --> READY (armed, green)
 *   READY    -- finger up  --> RUNNING (timer starts the instant the hand leaves)
 *   RUNNING  -- touch down --> STOPPED (final time shown)
 *   STOPPED  -- touch down --> READY (arm the next solve)
 *
 * Timing uses SystemClock.elapsedRealtime() (monotonic). The display refreshes once
 * per animation frame while running.
 */
class TimerFragment : Fragment() {

    private enum class State { IDLE, READY, RUNNING, STOPPED }

    private var binding: FragmentTimerBinding? = null
    private var state = State.IDLE
    private var startElapsed = 0L

    private val tick = object : Runnable {
        override fun run() {
            val b = binding ?: return
            b.timeText.text = format(SystemClock.elapsedRealtime() - startElapsed)
            b.timeText.postOnAnimation(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentTimerBinding.inflate(inflater, container, false)
        this.binding = binding

        binding.timerRoot.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> onPressDown()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onPressUp()
            }
            true
        }
        return binding.root
    }

    private fun onPressDown() {
        when (state) {
            State.RUNNING -> stop()
            State.IDLE, State.STOPPED -> arm()
            State.READY -> { /* already armed */ }
        }
    }

    private fun onPressUp() {
        if (state == State.READY) start()
    }

    private fun arm() {
        val b = binding ?: return
        state = State.READY
        b.timeText.text = getString(R.string.timer_initial)
        b.hintText.setText(R.string.timer_ready)
        b.timerRoot.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.timer_bg_ready))
    }

    private fun start() {
        val b = binding ?: return
        state = State.RUNNING
        startElapsed = SystemClock.elapsedRealtime()
        b.hintText.setText(R.string.timer_tap_to_stop)
        b.timerRoot.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.timer_bg_idle))
        b.timerRoot.keepScreenOn = true
        b.timeText.postOnAnimation(tick)
    }

    private fun stop() {
        val b = binding ?: return
        state = State.STOPPED
        b.timeText.removeCallbacks(tick)
        b.timeText.text = format(SystemClock.elapsedRealtime() - startElapsed)
        b.hintText.setText(R.string.timer_idle)
        b.timerRoot.keepScreenOn = false
    }

    override fun onPause() {
        super.onPause()
        // Freeze the value and release the screen lock when leaving the tab.
        if (state == State.RUNNING) stop()
    }

    override fun onDestroyView() {
        binding?.timeText?.removeCallbacks(tick)
        binding = null
        super.onDestroyView()
    }

    /** Formats milliseconds as S.cc or M:SS.cc (centisecond precision, WCA style). */
    private fun format(millis: Long): String {
        val totalCentis = millis / 10
        val centis = totalCentis % 100
        val totalSeconds = totalCentis / 100
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60
        return if (minutes > 0) {
            String.format(Locale.US, "%d:%02d.%02d", minutes, seconds, centis)
        } else {
            String.format(Locale.US, "%d.%02d", seconds, centis)
        }
    }
}
