package emortal.lazertag.gun

import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.item.Material

object Rifle : Gun("Rifle", 3) {

    override val material = Material.STONE_HOE
    override val color = NamedTextColor.GRAY

    override val damage = 10f
    override val cooldown = 300L
    override val ammo = 40
    override val maxDistance = 75.0

}