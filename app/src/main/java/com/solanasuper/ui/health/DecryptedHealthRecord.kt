package com.solanasuper.ui.health

data class DecryptedHealthRecord(
    val id: String,
    val title: String,
    val description: String,
    val date: Long, 
    val type: String = "General",
    val ipfsCid: String? = null
)
