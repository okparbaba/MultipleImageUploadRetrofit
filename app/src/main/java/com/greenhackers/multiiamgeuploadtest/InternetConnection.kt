package com.greenhackers.multiiamgeuploadtest

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi

import java.util.Objects



object InternetConnection {

    /**
     * CHECK WHETHER INTERNET CONNECTION IS AVAILABLE OR NOT
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun checkConnection(context: Context): Boolean {
        return (Objects.requireNonNull(context.getSystemService(Context.CONNECTIVITY_SERVICE)) as ConnectivityManager).activeNetworkInfo != null
    }
}