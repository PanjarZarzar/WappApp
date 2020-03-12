package com.canay.updatewhatsapp2019

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import timber.log.Timber
import java.io.File

class DownloadController(private val context: MainActivity, private val url: String) {

    private var onComplete: BroadcastReceiver? = null
    private var downloadId: Long? = null
    private var downloadManager: DownloadManager? = null
    private var isDownloadCancelled = false

    val downloadProgress = MutableLiveData<Float>()

    companion object {
        private const val FILE_NAME = "WhatsAppNew.apk"
        private const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"
        private const val APP_INSTALL_PATH = "\"application/vnd.android.package-archive\""

        const val INSTALL_REQUEST = 1
    }

    fun enqueueDownload() {

        var destination =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.toString() + "/"
        destination += FILE_NAME

        val uri = Uri.parse("$FILE_BASE_PATH$destination")

        val file = File(destination)
        if (file.exists()) file.delete()

        downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
        request.setMimeType(MIME_TYPE)
        request.setTitle(context.getString(R.string.title_file_download))
        request.setDescription(context.getString(R.string.downloading))

        // set destination
        request.setDestinationUri(uri)

        showInstallOption(destination, uri)
        // Enqueue a new download and same the referenceId
        downloadId = downloadManager?.enqueue(request)

        Timber.d("DownloadID: %s", downloadId)

        Thread(Runnable {
            var downloading = true

            while (downloading) {
                try {
                    val q = DownloadManager.Query()
                    q.setFilterById(downloadId!!)

                    val cursor = downloadManager?.query(q)
                    cursor?.moveToFirst()
                    val bytes_downloaded = cursor!!.getInt(
                        cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    )
                    val bytes_total =
                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false
                    }

                    val dl_progress = ((bytes_downloaded * 100L) / bytes_total).toFloat()

                    context.runOnUiThread(Runnable {
                        downloadProgress.value = dl_progress
                    })

                    cursor.close()

                } catch (e: Exception) {
                    Timber.d("Error Happenned")
                }

            }
        }).start()
    }

    private fun showInstallOption(
        destination: String,
        uri: Uri
    ) {

        // set BroadcastReceiver to install app when .apk is downloaded
        onComplete = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                downloadProgress.value = 0.0f

                if (isDownloadCancelled) {
                    return
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                        File(destination)
                    )
                    val install = Intent(Intent.ACTION_VIEW)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    install.data = contentUri
                    (context as Activity).startActivityForResult(install, INSTALL_REQUEST)
                    context.unregisterReceiver(this)
                    // finish()
                } else {
                    val install = Intent(Intent.ACTION_VIEW)
                    install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    install.setDataAndType(
                        uri,
                        APP_INSTALL_PATH
                    )
                    (context as Activity).startActivityForResult(install, INSTALL_REQUEST)
                    context.unregisterReceiver(this)
                    // finish()
                }
            }
        }
        onComplete?.apply {
            context.registerReceiver(this, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    fun deregisterReceiver() {
        onComplete?.apply {
            context.unregisterReceiver(this)
        }
    }

    fun cancelDownload() {
        downloadId?.apply {
            downloadManager?.remove(this)
            isDownloadCancelled = true
        }
    }
}
