package dev.emortal.lazertag.game

import dev.emortal.lazertag.LazerTagExtension

data class LazerTagGameOptions(
    val map: String = dev.emortal.lazertag.LazerTagExtension.maps.random()
)