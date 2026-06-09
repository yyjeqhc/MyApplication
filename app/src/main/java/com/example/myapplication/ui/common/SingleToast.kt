package com.example.myapplication.ui.common

import android.content.Context
import android.widget.Toast

private var currentToast: Toast? = null

fun showSingleToast(context: Context, message: String) {
    currentToast?.cancel()
    currentToast = Toast.makeText(
        context.applicationContext,
        message,
        Toast.LENGTH_SHORT
    ).also { it.show() }
}
