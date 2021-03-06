package com.ck.dev.tiptap.ui

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.ck.dev.tiptap.BuildConfig
import com.ck.dev.tiptap.data.AppDatabase
import com.ck.dev.tiptap.helpers.SharedPreferenceHelper
import timber.log.Timber

class GameApp:Application() {

    companion object{
        val hasCoinsUpdated = MutableLiveData<Boolean>()
        val hasGame1Played = MutableLiveData<Boolean>()
        val hasGame2Played = MutableLiveData<Boolean>()
        val hasGame3Played = MutableLiveData<Boolean>()
        val hasNameOrAvtarUpdated = MutableLiveData<Boolean>()
    }

    override fun onCreate() {
        Timber.i("onCreate called")
        super.onCreate()
        SharedPreferenceHelper.init(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String? {
                    return  String.format("(%s:%s)#%s",
                        element.fileName,
                        element.lineNumber,
                        element.methodName
                    );
                }
            })
        }
    }
}