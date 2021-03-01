package com.udacity.util.ext

import android.app.DownloadManager
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat

fun Context.getNotificationManager(): NotificationManager = ContextCompat.getSystemService(
    this,
    NotificationManager::class.java
) as NotificationManager

fun Context.getDownloadManager(): DownloadManager = ContextCompat.getSystemService(
    this,
    DownloadManager::class.java
) as DownloadManager