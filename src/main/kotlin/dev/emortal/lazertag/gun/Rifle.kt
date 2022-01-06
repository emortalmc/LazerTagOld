package dev.emortal.lazertag.gun

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent

object Rifle : Gun("Rifle") {

    override val material: Material = Material.STONE_HOE
    override val color: TextColor = NamedTextColor.GRAY

    override val damage = 6.9f
    override val cooldown = 5
    override val ammo = 30
    override val maxDistance = 75.0

    override val sound = Sound.sound(SoundEvent.ENTITY_PLAYER_BIG_FALL, Sound.Source.PLAYER, 1f, 0.75f)

}