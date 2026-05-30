package io.github.smigniot.andrubik

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.github.smigniot.andrubik.databinding.ActivityMainBinding

/**
 * Single tab-host Activity. The Scrambler and Timer live as Fragments behind a
 * persistent TabLayout + ViewPager2, so the tab selector stays put when switching
 * (no cross-Activity flicker). A future Solver tab plugs in via [Tab].
 */
class MainActivity : AppCompatActivity() {

    /** One entry per tab; add SOLVER here later. */
    private enum class Tab(val titleRes: Int, val create: () -> Fragment) {
        SCRAMBLER(R.string.title_scrambler, ::ScramblerFragment),
        TIMER(R.string.title_timer, ::TimerFragment),
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge safe: keep tabs below the status bar and content above the
        // navigation bar so nothing (e.g. the New scramble button) hides behind them.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        binding.viewPager.adapter = object : FragmentStateAdapter(this as FragmentActivity) {
            override fun getItemCount() = Tab.entries.size
            override fun createFragment(position: Int) = Tab.entries[position].create()
        }
        // Horizontal swipe is disabled so dragging the 3D cube doesn't change tabs;
        // tabs switch by tapping the TabLayout.
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.setText(Tab.entries[position].titleRes)
        }.attach()
    }
}
