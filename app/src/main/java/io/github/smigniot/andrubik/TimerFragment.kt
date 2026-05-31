package io.github.smigniot.andrubik

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.github.smigniot.andrubik.databinding.FragmentTimerBinding
import java.util.Locale

/**
 * Speedcubing timer with the standard frictionless interaction:
 *
 *   IDLE     -- touch down --> READY (armed, green)
 *   READY    -- finger up  --> RUNNING (timer starts the instant the hand leaves)
 *   RUNNING  -- touch down --> STOPPED (final time shown)
 *   STOPPED  -- screen touch ignored; only the explicit reset button --> IDLE
 *
 * Once stopped, screen touches are intentionally ignored so an accidental tap can't
 * dismiss the recorded time. A subtle reset button clears it for the next solve.
 *
 * Timing uses SystemClock.elapsedRealtime() (monotonic). The display refreshes once
 * per animation frame while running.
 */
class TimerFragment : Fragment() {

    private enum class State { IDLE, READY, RUNNING, STOPPED }

    private var binding: FragmentTimerBinding? = null
    private val viewModel: SyncViewModel by activityViewModels()
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
        binding.resetButton.setOnClickListener { reset() }
        binding.dismissButton.setOnClickListener { onDismissPressed() }
        return binding.root
    }

    private fun onPressDown() {
        when (state) {
            State.RUNNING -> stop()
            State.IDLE -> arm()
            // STOPPED ignores screen touches so the record isn't lost by accident;
            // READY is already armed.
            State.STOPPED, State.READY -> { /* no-op */ }
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
        // Offer a bail-out from the moment a solve attempt is armed.
        b.dismissButton.visibility = View.VISIBLE
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

    /**
     * Halts timing and shows the final value, but does NOT record it. Returns the
     * elapsed milliseconds so the caller decides whether to keep or discard it.
     */
    private fun stopTiming(): Long {
        val b = binding ?: return 0L
        state = State.STOPPED
        b.timeText.removeCallbacks(tick)
        val elapsed = SystemClock.elapsedRealtime() - startElapsed
        b.timeText.text = format(elapsed)
        b.timerRoot.keepScreenOn = false
        // Keep the result clean for a screenshot: hide the hint and the dismiss
        // button, reveal the subtle reset button as the only way to clear the time.
        b.hintText.visibility = View.INVISIBLE
        b.dismissButton.visibility = View.GONE
        b.resetButton.visibility = View.VISIBLE
        return elapsed
    }

    private fun stop() {
        val elapsed = stopTiming()
        // Hand the solve (scramble + time) to the sync pipeline.
        viewModel.recordSolve(elapsed)
    }

    /** Top-right dismiss button: bail out of the current attempt. */
    private fun onDismissPressed() {
        when (state) {
            State.RUNNING -> confirmDismiss()
            // Armed but not started: nothing recorded yet, just disarm.
            State.READY -> reset()
            State.IDLE, State.STOPPED -> { /* no-op */ }
        }
    }

    /**
     * Stops the timer right away (as any screen tap would, in case the button was
     * hit by mistake) and asks whether to discard the time. Confirming clears the
     * timer without recording or syncing; keeping records it like a normal stop.
     */
    private fun confirmDismiss() {
        val elapsed = stopTiming()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.timer_dismiss_title)
            .setMessage(R.string.timer_dismiss_message)
            .setPositiveButton(R.string.timer_dismiss_confirm) { _, _ -> reset() }
            .setNegativeButton(R.string.timer_dismiss_keep) { _, _ -> viewModel.recordSolve(elapsed) }
            .setOnCancelListener { viewModel.recordSolve(elapsed) }
            .show()
    }

    private fun reset() {
        val b = binding ?: return
        state = State.IDLE
        b.timeText.text = getString(R.string.timer_initial)
        b.hintText.setText(R.string.timer_idle)
        b.hintText.visibility = View.VISIBLE
        b.resetButton.visibility = View.GONE
        b.dismissButton.visibility = View.GONE
        b.timerRoot.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.timer_bg_idle))
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
