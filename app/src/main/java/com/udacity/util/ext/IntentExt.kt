package com.udacity.util.ext

import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import com.udacity.BuildConfig
import com.udacity.DownloadStatus

const val EXTRA_FILE_NAME = "${BuildConfig.APPLICATION_ID}.FILE_NAME"
const val EXTRA_DOWNLOAD_STATUS = "${BuildConfig.APPLICATION_ID}.DOWNLOAD_STATUS"

/**
 * Creates a [Bundle] with given parameters and pass as data to [com.udacity.DetailActivity].
 *
 * Further the extras can be extracted using either [EXTRA_FILE_NAME] and [EXTRA_DOWNLOAD_STATUS].
 */
fun Intent.createDetailExtras(
    fileName: String,
    downloadStatus: DownloadStatus
) = bundleOf(EXTRA_FILE_NAME to fileName, EXTRA_DOWNLOAD_STATUS to downloadStatus)