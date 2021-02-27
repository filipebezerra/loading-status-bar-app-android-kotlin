package com.udacity

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import com.udacity.ButtonState.Completed
import com.udacity.ButtonState.Loading
import com.udacity.databinding.MainActivityBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action
    private lateinit var viewBinding: MainActivityBinding
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<MainActivityBinding>(this, R.layout.main_activity)
            .apply {
                viewBinding = this
                setSupportActionBar(toolbar)
                registerReceiver(
                    onDownloadCompletedReceiver,
                    IntentFilter(ACTION_DOWNLOAD_COMPLETE)
                )
                onLoadingButtonClicked()
            }
    }

    private fun MainActivityBinding.onLoadingButtonClicked() {
        mainContent.loadingButton.setOnClickListener {
            if (mainContent.downloadOptionRadioGroup.checkedRadioButtonId == -1) {
                Toast.makeText(
                    this@MainActivity,
                    "Please select the file to download",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            requestDownload()
        }
    }

    private val onDownloadCompletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                Toast.makeText(
                    this@MainActivity,
                    "Download Completed",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private fun requestDownload() {
        val request = Request(Uri.parse(URL))
            .setTitle(getString(R.string.app_name))
            .setDescription(getString(R.string.app_description))
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID = downloadManager.enqueue(request)

        handler = Handler(Looper.getMainLooper())
        contentResolver.registerContentObserver(
            "content://downloads/my_downloads".toUri(),
            true,
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) = downloadManager.queryProgress()
            }
        )
    }

    private fun DownloadManager.queryProgress() {
        query(Query().setFilterById(downloadID)).use {
            with(it) {
                if (this != null && moveToFirst()) {
                    when (getInt(getColumnIndex(COLUMN_STATUS))) {
                        STATUS_FAILED -> {
                            Timber.d("Download failed")
                            viewBinding.mainContent.loadingButton.changeButtonState(Completed)
                        }
                        STATUS_PAUSED -> {
                            Timber.d("Download paused")
                        }
                        STATUS_PENDING -> {
                            Timber.d("Download pending")
                        }
                        STATUS_RUNNING -> {
                            Timber.d("Download running")
                            viewBinding.mainContent.loadingButton.changeButtonState(Loading)
                        }
                        STATUS_SUCCESSFUL -> {
                            Timber.d("Download successful")
                            viewBinding.mainContent.loadingButton.changeButtonState(Completed)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadCompletedReceiver)
    }

    companion object {
        private const val URL =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
        private const val CHANNEL_ID = "channelId"
    }
}
