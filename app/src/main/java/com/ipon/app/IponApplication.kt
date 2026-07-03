package com.ipon.app

import android.app.Application
import com.ipon.app.di.IponAppContainer

class IponApplication : Application() {
    lateinit var container: IponAppContainer

    override fun onCreate() {
        super.onCreate()
        container = IponAppContainer(this)
    }
}
