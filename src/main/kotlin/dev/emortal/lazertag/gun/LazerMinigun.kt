package dev.emortal.lazertag.gun

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent

object LazerMinigun : Gun("Lazer Minigun") {

    override val material: Material = Material.NETHERITE_SHOVEL
    override val color: TextColor = NamedTextColor.AQUA

    override val damage = 0.75f
    override val cooldown = 100L
    override val ammo = 100
    override val reloadTime = 5000L
    override val maxDistance = 50.0

    override val burstAmount = 4
    override val burstInterval = 20L

    override val spread = 0.0

    override val sound = Sound.sound(SoundEvent.BLOCK_BEACON_DEACTIVATE, Sound.Source.PLAYER, 2f, 2f)

}