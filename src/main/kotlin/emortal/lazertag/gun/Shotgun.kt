package emortal.lazertag.gun

import emortal.lazertag.utils.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.time.TimeUnit
import java.time.Duration

object Shotgun : Gun("Shotgun", 2) {

    override val material = Material.REPEATER
    override val color: TextColor = NamedTextColor.RED

    override val damage = 2f
    override val numberOfBullets = 25
    override val spread = 0.15
    override val cooldown = 500L
    override val ammo = 5
    override val reloadTime = 2000L
    override val maxDistance = 30.0

    override val sound = Sound.sound(SoundEvent.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, Sound.Source.PLAYER, 1.5f, 1f)

    override fun shootAfter(player: Player) {
        player.velocity = player.position.direction().normalize().mul(-20.0)

        object : MinestomRunnable() {
            var i = 0

            override fun run() {
                if (i >= 2) {
                    cancel()
                    return
                }

                player.playSound(Sound.sound(SoundEvent.ENTITY_IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))

                i++
            }
        }.delay(Duration.of(5L, TimeUnit.SERVER_TICK)).repeat(Duration.of(3L, TimeUnit.SERVER_TICK)).schedule()
    }

}