package dev.emortal.lazertag.gun

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.item.Material

object SMG : Gun("SMG") {

    override val material: Material = Material.IRON_HOE
    override val color: TextColor = NamedTextColor.YELLOW
    override val damage = 2.75f
    override val cooldown: Int = 120
    override val ammo = 60
    override val spread = 0.2
    override val reloadTime: Int = 3000

    override val maxDistance = 15.0

    override val burstAmount = 4
    override val burstInterval: Int = 50

}