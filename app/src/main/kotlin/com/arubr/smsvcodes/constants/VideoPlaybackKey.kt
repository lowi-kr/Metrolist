package com.arubr.smsvcodes.constants

import androidx.datastore.preferences.core.booleanPreferencesKey

/**
 * Whether to play the video stream when a YouTube Music track has a music video available.
 * Defaults to false — audio-only behaviour is preserved unless the user opts in.
 */
val VideoPlaybackKey = booleanPreferencesKey("video_playback_enabled")
