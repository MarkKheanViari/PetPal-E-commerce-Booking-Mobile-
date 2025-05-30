package com.example.myapplication

data class ServiceModel(
    val name: String,
    val price: String,
    val description: String,
    val type: String,
    val image: String,
    val startTime: String?,
    val endTime: String?,
    val startDay: String?, // New
    val endDay: String?    // New
)