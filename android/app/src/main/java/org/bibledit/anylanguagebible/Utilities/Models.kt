package org.bibledit.anylanguagebible.Utilities

import kotlinx.serialization.Serializable

@Serializable
data class Module(
    val abbrev: String,
    val id: Int,
    val name: String,
    val source: String,
    val type: Int,
    val uuid: String,
    val version: String
)



@Serializable
data class BibleVerse(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String
)

@Serializable
data class BibleFile(
    val data: List<BibleVerse>
)



