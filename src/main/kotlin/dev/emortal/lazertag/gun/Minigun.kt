package dev.emortal.lazertag.gun

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent

object Minigun : Gun("Minigun") {

    override val material: Material = Material.DIAMOND_SHOVEL
    override val color: TextColor = NamedTextColor.AQUA

    override val damage = 0.75f
    override val cooldown: Int = 170
    override val ammo = 70
    override val reloadTime: Int = 3000
    override val maxDistance = 40.0

    override val burstAmount = 4
    override val burstInterval: Int = 50

    override val spread = 0.025

    override val sound = Sound.sound(SoundEvent.ENTITY_ARMOR_STAND_HIT, Sound.Source.PLAYER, 1f, 1f)

}