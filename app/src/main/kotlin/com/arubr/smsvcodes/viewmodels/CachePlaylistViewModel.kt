/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.arubr.smsvcodes.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.cache.SimpleCache
import com.arubr.smsvcodes.constants.HideExplicitKey
import com.arubr.smsvcodes.constants.HideVideoSongsKey
import com.arubr.smsvcodes.db.MusicDatabase
import com.arubr.smsvcodes.db.entities.Song
import com.arubr.smsvcodes.di.DownloadCache
import com.arubr.smsvcodes.di.PlayerCache
import com.arubr.smsvcodes.extensions.filterExplicit
import com.arubr.smsvcodes.extensions.filterVideoSongs
import com.arubr.smsvcodes.utils.dataStore
import com.arubr.smsvcodes.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CachePlaylistViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
        @PlayerCache private val playerCache: SimpleCache,
        @DownloadCache private val downloadCache: SimpleCache,
    ) : ViewModel() {
        private val _cachedSongs = MutableStateFlow<List<Song>>(emptyList())
        val cachedSongs: StateFlow<List<Song>> = _cachedSongs

        init {
            viewModelScope.launch {
                while (true) {
                    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                    val cachedIds = playerCache.keys.toSet()
                    val downloadedIds = downloadCache.keys.toSet()
                    val pureCacheIds = cachedIds.subtract(downloadedIds)

                    val songs =
                        if (pureCacheIds.isNotEmpty()) {
                            database.getSongsByIds(pureCacheIds.toList())
                        } else {
                            emptyList()
                        }

                    val completeSongs =
                        songs.filter {
                            val contentLength = it.format?.contentLength
                            contentLength != null && playerCache.isCached(it.song.id, 0, contentLength)
                        }

                    if (completeSongs.isNotEmpty()) {
                        database.query {
                            completeSongs.forEach {
                                if (it.song.dateDownload == null) {
                                    update(it.song.copy(dateDownload = LocalDateTime.now()))
                                }
                            }
                        }
                    }

                    _cachedSongs.value =
                        completeSongs
                            .filter { it.song.dateDownload != null }
                            .sortedByDescending { it.song.dateDownload }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)

                    delay(1000)
                }
            }
        }

        fun removeSongFromCache(songId: String) {
            playerCache.removeResource(songId)
        }
    }
