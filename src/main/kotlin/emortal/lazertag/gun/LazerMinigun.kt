package emortal.lazertag.gun

import net.kyori.adventure.sound.Sound
import net.minestom.server.sound.SoundEvent

object LazerMinigun : Gun("Lazer Minigun", 1) {

    override val damage = 1f
    override val cooldown = 150L
    override val ammo = 60
    override val reloadTime = 2000L
    override val maxDistance = 25.0

    override val burstAmount = 5
    override val burstInterval = 1L

    override val sound = Sound.sound(SoundEvent.ARMOR_STAND_HIT, Sound.Source.PLAYER, 1f, 1f)

}