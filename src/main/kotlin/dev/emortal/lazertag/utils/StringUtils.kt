package dev.emortal.lazertag.utils

fun String.title(): String = this.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }