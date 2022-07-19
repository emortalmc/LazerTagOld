package dev.emortal.lazertag.game

data class LazerTagGameOptions(
    val map: String = dev.emortal.lazertag.LazerTagExtension.maps.random()
)