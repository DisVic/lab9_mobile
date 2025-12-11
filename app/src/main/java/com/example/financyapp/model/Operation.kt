package com.example.financyapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Operation(
    val id: Int? = null,
    val type: String,
    val amount: Double,
    val category: String,
    val date: String,
    val note: String
) : Parcelable