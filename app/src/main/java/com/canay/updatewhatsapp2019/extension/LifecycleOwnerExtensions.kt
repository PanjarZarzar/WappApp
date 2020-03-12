package com.vimcar.cars.extension

import androidx.lifecycle.*

/**
 * Extension function to make observing LiveData objects more comfortable.
 */
fun <T : Any, L : LiveData<T>> LifecycleOwner.observe(liveData: L, body: (T?) -> Unit) : LiveData<T> {
    liveData.observe(this, Observer(body))
    return liveData
}