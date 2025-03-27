package com.example.myapplication

data class Appointment(
    val serviceName: String,
    val serviceType: String,
    val appointmentDate: String,
    val price: String,
    val status: String,
    val image: String,
    val petName: String? = null,
    val petBreed: String? = null,
    val notes: String? = null,
    val paymentMethod: String? = null
)