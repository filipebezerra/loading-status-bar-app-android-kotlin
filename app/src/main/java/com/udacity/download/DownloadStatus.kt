package com.udacity.download

enum class DownloadStatus(val statusText: String) {
    SUCCESSFUL("Successful"), FAILED("Failed"), UNKNOWN("Unknown")
}