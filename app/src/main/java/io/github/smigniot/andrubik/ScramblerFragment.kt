package io.github.smigniot.andrubik

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import io.github.smigniot.andrubik.databinding.FragmentScramblerBinding

/**
 * Hosts the cubing.js scrambler (scramble generation + <twisty-player>) inside a
 * WebView. Assets are bundled under app/src/main/assets/web/ and served over the
 * virtual https://appassets.androidplatform.net/ origin via WebViewAssetLoader.
 *
 * We must NOT use file:// — cubing.js relies on Web Workers + WebAssembly, which
 * are blocked from opaque file:// origins. The asset-loader origin behaves like a
 * real https origin, so workers/wasm/modules load correctly.
 */
class ScramblerFragment : Fragment() {

    private var binding: FragmentScramblerBinding? = null
    private val viewModel: SyncViewModel by activityViewModels()

    /** Receives the current scramble from cubing.js so the Timer can attach it to a solve. */
    private inner class ScrambleBridge {
        @JavascriptInterface
        fun onScramble(scramble: String) {
            viewModel.currentScramble = scramble
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentScramblerBinding.inflate(inflater, container, false)
        this.binding = binding

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .build()

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // Only our bundled local assets are ever loaded, so exposing this
            // single-method bridge is safe.
            addJavascriptInterface(ScrambleBridge(), "AndRubikNative")
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                override fun onPageFinished(view: WebView, url: String) {
                    this@ScramblerFragment.binding?.progress?.visibility = View.GONE
                }
            }
            loadUrl("https://appassets.androidplatform.net/assets/web/index.html")
        }
        return binding.root
    }

    override fun onDestroyView() {
        binding?.webView?.apply {
            loadUrl("about:blank")
            destroy()
        }
        binding = null
        super.onDestroyView()
    }
}
