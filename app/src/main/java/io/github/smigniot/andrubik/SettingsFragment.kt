package io.github.smigniot.andrubik

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.github.smigniot.andrubik.databinding.FragmentSettingsBinding

/** Sync configuration: toggle, server URL, credentials. Persisted via [Settings]. */
class SettingsFragment : Fragment() {

    private var binding: FragmentSettingsBinding? = null
    private val viewModel: SyncViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        this.binding = binding
        val settings = Settings(requireContext())

        binding.syncCheckbox.isChecked = settings.syncEnabled
        binding.urlInput.setText(settings.serverUrl)
        binding.userInput.setText(settings.username)
        binding.passInput.setText(settings.password)

        binding.saveButton.setOnClickListener {
            settings.syncEnabled = binding.syncCheckbox.isChecked
            settings.serverUrl = binding.urlInput.text?.toString()?.trim().orEmpty()
            settings.username = binding.userInput.text?.toString().orEmpty()
            settings.password = binding.passInput.text?.toString().orEmpty()
            Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
            // Saving may have enabled sync or fixed the URL — retry the queue now.
            viewModel.syncNow()
        }

        viewModel.pendingCount.observe(viewLifecycleOwner) { count ->
            binding.statusText.text = getString(R.string.settings_pending, count)
        }
        return binding.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
