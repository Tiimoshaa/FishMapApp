package com.example.fishapp

data class MarkerData(
    val id: String? = null, // Уникальный идентификатор маркера
    val title: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val username: String = "",
    val species: String = "",
    val massRange: String = ""
){

}
