package emortal.lazertag.gun

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent

object Minigun : Gun("Lazer Minigun") {

    override val material: Material = Material.DIAMOND_SHOVEL
    override val color: TextColor = NamedTextColor.AQUA

    override val damage = 1.5f
    override val cooldown = 150L
    override val ammo = 50
    override val reloadTime = 2500L
    override val maxDistance = 30.0

    override val burstAmount = 5
    override val burstInterval = 1L

    override val sound = Sound.sound(SoundEvent.ENTITY_ARMOR_STAND_HIT, Sound.Source.PLAYER, 1f, 1f)

}