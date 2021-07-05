package emortal.lazertag.maps

sealed class LazerTagMap(val name: String) {

    companion object {
        val maps = LazerTagMap::class.sealedSubclasses.map { it.objectInstance!! }
    }

}