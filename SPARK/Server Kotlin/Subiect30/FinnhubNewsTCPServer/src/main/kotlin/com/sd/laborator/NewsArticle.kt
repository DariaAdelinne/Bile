package com.sd.laborator

import kotlinx.serialization.Serializable

@Serializable
data class NewsArticle(
    val symbol: String,
    val category: String,
    val datetime: Long,      // Unix timestamp
    val headline: String,
    val id: Long,
    val image: String,       // URL-ul imaginii (poate fi .png)
    val related: String,
    val source: String,
    val summary: String,
    val url: String
)
