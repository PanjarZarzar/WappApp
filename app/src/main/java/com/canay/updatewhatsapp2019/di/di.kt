package com.canay.updatewhatsapp2019.di

import android.app.Application
import android.se.omapi.Session
import com.canay.updatewhatsapp2019.MainActivity
import com.canay.updatewhatsapp2019.MainActivityViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun Application.initDI() {
    startKoin {
        androidLogger()
        androidContext(this@initDI)
        modules(listOf(scopesModule))
    }
}

private val scopesModule = module {
    scope(named("MainActivityScope")) {

        viewModel { MainActivityViewModel() }
    }
}

//private val dataModule = module {
//    single<RetrofitFactory> { RetrofitFactoryImpl() }
//    single<VehicleManager> {VehicleManagerImpl(get())}
//}