package com.udacity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.DataBindingUtil.setContentView
import com.udacity.databinding.DetailActivityBinding

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView<DetailActivityBinding>(this, R.layout.detail_activity)
            .apply {
                setSupportActionBar(toolbar)
                intent?.extras?.let {
                    with(detailContent) {
                        fileNameText.text = it.getString(EXTRA_FILE_NAME)
                        downloadStatusText.text = it.getString(EXTRA_DOWNLOAD_STATUS)
                    }
                }
            }
    }

    companion object {
        private const val EXTRA_FILE_NAME = "${BuildConfig.APPLICATION_ID}.FILE_NAME"
        private const val EXTRA_DOWNLOAD_STATUS = "${BuildConfig.APPLICATION_ID}.DOWNLOAD_STATUS"

        /**
         * Creates a [Bundle] with given parameters and pass as data to [DetailActivity].
         */
        fun bundleExtrasOf(
            fileName: String,
            downloadStatus: DownloadStatus
        ) = bundleOf(
            EXTRA_FILE_NAME to fileName,
            EXTRA_DOWNLOAD_STATUS to downloadStatus.statusText
        )
    }
}