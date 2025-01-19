package com.example.kiparys.data.model

import androidx.annotation.Keep

@Keep
data class BottomSheetOption @Keep constructor(
    val title: String,
    val iconRes: Int,
    val tag: String
)
