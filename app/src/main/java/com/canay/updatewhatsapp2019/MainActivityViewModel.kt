package com.canay.updatewhatsapp2019

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class MainActivityViewModel : ViewModel() {

    private val _availableVersion = MutableLiveData<String>()
    private val _downloadLink = MutableLiveData<String>()

    val availableVersion: LiveData<String> = _availableVersion
    val downloadLink: LiveData<String> = _downloadLink

     fun getAvailableWebVersion() {
        viewModelScope.launch {
            val downLoadDocument = withContext(Dispatchers.IO) {
                Jsoup.connect("https://whatsapp-messenger.en.uptodown.com/android/download")
                    .get()
            }

            downLoadDocument?.apply {
                _availableVersion.value =
                    this.getElementsByClass("version").first().allElements[1].text()
                _downloadLink.value = this.getElementsByClass("data download").attr("abs:href")
            }
        }
    }
}