package com.canay.updatewhatsapp2019.util

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

/*
 * Copyright (c) 2018 T-Systems Multimedia Solutions GmbH
 * Riesaer Str. 5, D-01129 Dresden, Germany
 * All rights reserved.
 *
 * Autor: jaab
 * Datum: 8/19/2019
 */

// Returns current network state
object InitialNetworkState {
    fun hasNetwrokConnection(application: Application): Boolean {
        val connectivityManager: ConnectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }
}