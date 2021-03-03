package com.udacity.download

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.udacity.R
import com.udacity.util.ext.createDownloadStatusChannel
import com.udacity.util.ext.getNotificationManager
import com.udacity.util.ext.sendDownloadCompletedNotification
import timber.log.Timber

class DownloadNotificator(private val context: Context, private val lifecycle: Lifecycle) :
    LifecycleObserver {

    init {
        lifecycle.addObserver(this).also {
            Timber.d("Notificator added as a Lifecycle Observer")
        }
    }

    fun notify(
        fileName: String,
        downloadStatus: DownloadStatus,
    ) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            Timber.d("Notifying with a Toast. Lifecycle is resumed")
            Toast.makeText(
                context,
                context.getString(R.string.download_completed),
                Toast.LENGTH_SHORT
            ).show();
        }
        with(context.applicationContext) {
            getNotificationManager().run {
                createDownloadStatusChannel(applicationContext)
                sendDownloadCompletedNotification(
                    fileName,
                    downloadStatus,
                    applicationContext
                )
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun unregisterObserver() = lifecycle.removeObserver(this)
        .also { Timber.d("Notificator removed from Lifecycle Observers") }
}