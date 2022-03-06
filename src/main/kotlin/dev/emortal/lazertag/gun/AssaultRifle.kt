package dev.emortal.lazertag.gun

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.item.Material

object AssaultRifle : Gun("Assault Rifle") {

    override val material: Material = Material.IRON_HOE
    override val color: TextColor = NamedTextColor.YELLOW
    override val damage = 3.5f
    override val cooldown = 320L
    override val ammo = 40
    override val reloadTime = 3000L

    override val maxDistance = 40.0

    override val burstAmount = 2
    override val burstInterval = 170L

}