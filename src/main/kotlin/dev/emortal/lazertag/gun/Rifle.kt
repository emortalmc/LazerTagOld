package dev.emortal.lazertag.gun

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent

object Rifle : Gun("Rifle") {

    override val material: Material = Material.STONE_HOE
    override val color: TextColor = NamedTextColor.GRAY

    override val damage = 6.67f
    override val cooldown: Int = 300
    override val ammo = 15
    override val reloadTime: Int = 2500
    override val maxDistance = 100.0
    override val headshotModifier = 5f

    override val sound = Sound.sound(SoundEvent.ENTITY_PLAYER_BIG_FALL, Sound.Source.PLAYER, 1f, 0.75f)

}