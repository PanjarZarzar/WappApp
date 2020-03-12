package com.canay.updatewhatsapp2019

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.canay.updatewhatsapp2019.extension.checkSelfPermissionCompat
import com.canay.updatewhatsapp2019.extension.requestPermissionsCompat
import com.canay.updatewhatsapp2019.extension.shouldShowRequestPermissionRationaleCompat
import com.canay.updatewhatsapp2019.extension.showSnackbar
import com.canay.updatewhatsapp2019.util.CommonUtil
import com.canay.updatewhatsapp2019.util.ConnectivityLiveData
import com.canay.updatewhatsapp2019.util.InitialNetworkState
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.vimcar.cars.extension.observe
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private var installedVersion: String? = null
    private var availableVersion: String? = null

    private var isNetworkConnected = false

    private var buttonState = ButtonState.CHECK

    private lateinit var viewModel: MainActivityViewModel

    private lateinit var mInterstitialAd: InterstitialAd

    companion object {
        const val PERMISSION_REQUEST_STORAGE = 0
    }

    private var downloadController: DownloadController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        isNetworkConnected = InitialNetworkState.hasNetwrokConnection(application)

        observe(ConnectivityLiveData(application)) { state ->
            isNetworkConnected = state!!
            Timber.d("Network Connection: $isNetworkConnected")
        }

        if (isNetworkConnected) {
            viewModel.getAvailableWebVersion()
        } else {
            mainLayout.showSnackbar(
                R.string.Internet_Connection_Error,
                Snackbar.LENGTH_INDEFINITE, R.string.ok
            ) {}
        }

        observe(viewModel.availableVersion) {
            availableVersionTextView.text = it
            availableVersion = it
            compareVersions()
        }

        observe(viewModel.downloadLink) { it ->
            it?.apply {
                downloadController = DownloadController(this@MainActivity, it)

                downloadController?.apply {
                    observe(this.downloadProgress){progress->
                        Timber.d("Download Progress: $it")
                        progressBar.progress = progress!!
                        progressCountTextView.text = "" + progress.toInt() + "%"
                        if(progress==100.0f){
                            updateButtonState(ButtonState.CHECK)
                        }
                    }
                }
            }
        }

        commonButton.setOnClickListener {
            when (buttonState) {
                ButtonState.CHECK -> {
                    if (isNetworkConnected) {
                        infoTextView.text = getString(R.string.checking_state)
                        getCurrentVersion()
                        viewModel.getAvailableWebVersion()
                    } else {
                        mainLayout.showSnackbar(
                            R.string.Internet_Connection_Error,
                            Snackbar.LENGTH_INDEFINITE, R.string.ok
                        ) {}
                    }
                }

                ButtonState.DOWNLOAD -> {
                    if (isNetworkConnected) {
                        checkStoragePermission()
                    } else {
                        mainLayout.showSnackbar(
                            R.string.Internet_Connection_Error,
                            Snackbar.LENGTH_INDEFINITE, R.string.ok
                        ) {}
                    }
                }

                ButtonState.CANCEL -> {
                    downloadController?.cancelDownload()
                    updateButtonState(ButtonState.CHECK)
                }
            }
        }

        MobileAds.initialize(this) {}


        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "Add interstitial app key"

        mInterstitialAd.loadAd(AdRequest.Builder().build())

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        getCurrentVersion()
    }

    public override fun onDestroy() {
        super.onDestroy()
        downloadController?.deregisterReceiver()
    }

    private fun compareVersions() {
        if (CommonUtil.compareVersion(installedVersion, availableVersion)) {
            backLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
            updateButtonState(ButtonState.DOWNLOAD)
        } else {
            backLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            updateButtonState(ButtonState.CHECK)
            infoTextView.text = getString(R.string.alreadyLatestVersion)
        }
    }

    private fun updateButtonState(state: ButtonState){
        when(state) {
            ButtonState.DOWNLOAD -> {
                appIconImageView.visibility = View.VISIBLE
                progressBarLayout.visibility = View.GONE
                buttonState = ButtonState.DOWNLOAD
                progressBar.progress = 0.0f
                infoTextView.text = getString(R.string.newVersionAvailable)
                commonButton.text = getString(R.string.button_download)
            }
            ButtonState.CHECK -> {
                appIconImageView.visibility = View.VISIBLE
                progressBarLayout.visibility = View.GONE
                progressBar.progress = 0.0f
                buttonState = ButtonState.CHECK
                commonButton.text = getString(R.string.button_check)
                infoTextView.text = ""
            }
            ButtonState.CANCEL ->{
                appIconImageView.visibility = View.GONE
                progressBarLayout.visibility = View.VISIBLE
                buttonState = ButtonState.CANCEL
                infoTextView.text = getString(R.string.downloading)
                commonButton.text = getString(R.string.button_cancel)
            }
        }
    }

    private fun getCurrentVersion() {
        val pm = packageManager

        val packages = pm.getInstalledPackages(0)
        for (packageInfo in packages) {
            if (packageInfo.packageName == "com.whatsapp") {
                installedVersion = packageInfo.versionName
                installedVersionTextView.text = installedVersion
            }
        }
    }

    private fun performDownload() {
        downloadController?.enqueueDownload()
        updateButtonState(ButtonState.CANCEL)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            // Request for camera permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // start downloading
                performDownload()
            } else {
                // Permission request was denied.
                mainLayout.showSnackbar(R.string.storage_permission_denied, Snackbar.LENGTH_SHORT)
            }
        }
    }


    private fun checkStoragePermission() {
        // Check if the storage permission has been granted
        if (checkSelfPermissionCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            // start downloading
            performDownload()
        } else {
            // Permission is missing and must be requested.
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {

        if (shouldShowRequestPermissionRationaleCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            mainLayout.showSnackbar(
                R.string.storage_access_required,
                Snackbar.LENGTH_INDEFINITE, R.string.ok
            ) {
                requestPermissionsCompat(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_STORAGE
                )
            }

        } else {
            requestPermissionsCompat(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_STORAGE
            )
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to exit?")
            .setCancelable(true)
            .setPositiveButton(
                "Yes"
            ) { dialog, id ->
                System.exit(1)
            }

            .setNegativeButton("No") { dialog, id ->
                if (mInterstitialAd.isLoaded) {
                    mInterstitialAd.show()
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.")
                }
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == DownloadController.INSTALL_REQUEST){
            if (mInterstitialAd.isLoaded) {
                mInterstitialAd.show()
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }
        }
    }

}