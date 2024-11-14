package com.cs407.statify

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cs407.statify.R

class LoadingFragment : Fragment() {

    private lateinit var videoView: VideoView
    private var isAppReady: Boolean = false
    private val minimumDisplayTime = 6000L
    private var startTime: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoView = view.findViewById(R.id.loadingVideoView)
        val videoUri = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.statifyloadingani}")
        videoView.setVideoURI(videoUri)

        startTime = System.currentTimeMillis()

        videoView.start()
        
        checkAppReady()

        videoView.setOnCompletionListener {
            if (isAppReady && hasMinimumDisplayTimeElapsed()) {
                navigateToHome()
            } else {
                videoView.start()
            }
        }
    }

    private fun checkAppReady() {
        Handler(Looper.getMainLooper()).postDelayed({
            isAppReady = true
            if (videoView.isPlaying && hasMinimumDisplayTimeElapsed()) {
                videoView.stopPlayback()
                navigateToHome()
            }
        }, 6000)
    }

    private fun hasMinimumDisplayTimeElapsed(): Boolean {
        val elapsedTime = System.currentTimeMillis() - startTime
        return elapsedTime >= minimumDisplayTime
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_loadingFragment_to_homeFragment)
    }
}