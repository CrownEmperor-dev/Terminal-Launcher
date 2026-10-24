package com.example.terminallauncher.data

import android.graphics.drawable.Drawable

data class AppModel(
    val label: String,
    val packageName: String,
    val className: String,
    val icon: Drawable? = null
)
