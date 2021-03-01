package com.udacity

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import com.udacity.ButtonState.Completed
import com.udacity.ButtonState.Loading
import com.udacity.databinding.MainActivityBinding
import com.udacity.util.ext.createDownloadStatusChannel
import com.udacity.util.ext.getDownloadManager
import com.udacity.util.ext.getNotificationManager
import com.udacity.util.ext.sendDownloadCompletedNotification
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: MainActivityBinding

    private var downloadFileName = ""
    private var downloadID: Long = NO_DOWNLOAD
    private var downloadContentObserver: ContentObserver? = null

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
        with(mainContent) {
            loadingButton.setOnClickListener {
                when (downloadOptionRadioGroup.checkedRadioButtonId) {
                    View.NO_ID ->
                        Toast.makeText(
                            this@MainActivity,
                            "Please select the file to download",
                            Toast.LENGTH_SHORT
                        ).show()
                    else -> {
                        downloadFileName =
                            findViewById<RadioButton>(downloadOptionRadioGroup.checkedRadioButtonId)
                                .text.toString()
                        requestDownload()
                    }
                }
            }
        }
    }

    private val onDownloadCompletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
            id?.let {
                val downloadStatus = getDownloadManager().queryStatus(it)

                Timber.d("Download $it completed with status: ${downloadStatus.statusText}")
                downloadStatus.takeIf { status -> status != DownloadStatus.UNKNOWN }?.run {
                    getNotificationManager().run {
                        createDownloadStatusChannel(applicationContext)
                        sendDownloadCompletedNotification(
                            downloadFileName,
                            downloadStatus,
                            applicationContext
                        )
                    }
                }
            }

            //if (downloadID == id) {

            //unregisterDownloadContentObserver()


//                Toast.makeText(
//                    this@MainActivity,
//                    "Download Completed",
//                    Toast.LENGTH_SHORT
//                ).show();
            //}
        }
    }

    private fun DownloadManager.queryStatus(id: Long): DownloadStatus {
        query(Query().setFilterById(id)).use {
            with(it) {
                if (this != null && moveToFirst()) {
                    return when (getInt(getColumnIndex(COLUMN_STATUS))) {
                        STATUS_SUCCESSFUL -> DownloadStatus.SUCCESSFUL
                        STATUS_FAILED -> DownloadStatus.FAILED
                        else -> DownloadStatus.UNKNOWN
                    }
                }
                return DownloadStatus.UNKNOWN
            }
        }
    }

    private fun requestDownload() {
        with(getDownloadManager()) {
            downloadID.takeIf { it != NO_DOWNLOAD }?.run {
                val downloadsCancelled = remove(downloadID)
                unregisterDownloadContentObserver()
                downloadID = NO_DOWNLOAD
                Timber.d("Number of downloads cancelled: $downloadsCancelled")
            }

            val request = Request(Uri.parse(URL))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadID = enqueue(request)

            createAndRegisterDownloadContentObserver()
        }
    }

    private fun DownloadManager.createAndRegisterDownloadContentObserver() {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                downloadContentObserver?.run { queryProgress() }
            }
        }.also {
            downloadContentObserver = it
            contentResolver.registerContentObserver(
                "content://downloads/my_downloads".toUri(),
                true,
                downloadContentObserver!!
            )
        }
    }

    private fun DownloadManager.queryProgress() {
        query(Query().setFilterById(downloadID)).use {
            with(it) {
                if (this != null && moveToFirst()) {
                    val id = getInt(getColumnIndex(COLUMN_ID))
                    when (getInt(getColumnIndex(COLUMN_STATUS))) {
                        STATUS_FAILED -> {
                            Timber.d("Download $id: failed")
                            viewBinding.mainContent.loadingButton.changeButtonState(Completed)
                        }
                        STATUS_PAUSED -> {
                            Timber.d("Download $id: paused")
                        }
                        STATUS_PENDING -> {
                            Timber.d("Download $id: pending")
                        }
                        STATUS_RUNNING -> {
                            Timber.d("Download $id: running")
                            viewBinding.mainContent.loadingButton.changeButtonState(Loading)
                        }
                        STATUS_SUCCESSFUL -> {
                            Timber.d("Download $id: successful")
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
        unregisterDownloadContentObserver()
    }

    private fun unregisterDownloadContentObserver() {
        downloadContentObserver?.let {
            contentResolver.unregisterContentObserver(it)
            downloadContentObserver = null
        }
    }

    companion object {
        private const val URL =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
        private const val NO_DOWNLOAD = 0L
    }
}
