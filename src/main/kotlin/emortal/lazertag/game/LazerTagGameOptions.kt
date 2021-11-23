package emortal.lazertag.game

import emortal.lazertag.LazerTagExtension

data class LazerTagGameOptions(
    val map: String = LazerTagExtension.maps.random()
)