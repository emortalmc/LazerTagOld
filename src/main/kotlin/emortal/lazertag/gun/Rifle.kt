package emortal.lazertag.gun

object Rifle : Gun("Rifle", 3) {

    override val damage = 10f
    override val cooldown = 300L
    override val ammo = 40
    override val maxDistance = 75.0

}