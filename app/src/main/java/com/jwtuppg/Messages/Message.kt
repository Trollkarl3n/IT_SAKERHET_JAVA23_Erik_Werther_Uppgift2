package com.jwtuppg.Messages

data class Message(
    val id: String = "",
    val user: String = "",  // Lägg till användarnamn eller användar-ID
    val content: String = "",
    val timestamp: Long = 0L
)