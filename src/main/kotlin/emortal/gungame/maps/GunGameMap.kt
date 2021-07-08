package emortal.gungame.maps

sealed class GunGameMap(val name: String) {

    companion object {
        val maps = GunGameMap::class.sealedSubclasses.map { it.objectInstance!! }
    }

}