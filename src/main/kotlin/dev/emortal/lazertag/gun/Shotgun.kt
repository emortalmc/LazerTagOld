package dev.emortal.lazertag.gun

import dev.emortal.immortal.game.Game
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import java.util.function.Supplier

object Shotgun : Gun("Shotgun") {

    override val material = Material.REPEATER
    override val color: TextColor = NamedTextColor.RED

    override val damage = 1.25f
    override val numberOfBullets = 20
    override val spread = 0.13
    override val cooldown: Int = 400
    override val ammo = 6
    override val reloadTime: Int = 3300
    override val freshReload = false
    override val shootMidReload = false
    override val maxDistance = 25.0

    override val sound = Sound.sound(SoundEvent.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, Sound.Source.PLAYER, 1.5f, 1f)

    override fun shootAfter(game: Game, player: Player) {
        player.velocity = player.position.direction().normalize().mul(-15.0)

        player.scheduler().submitTask(object : Supplier<TaskSchedule> {
            var iter = 2

            override fun get(): TaskSchedule {
                if (iter == 2) {
                    iter--
                    return TaskSchedule.tick(5)
                }
                if (iter == 0) {
                    return TaskSchedule.stop()
                }
                iter--

                player.playSound(Sound.sound(SoundEvent.ENTITY_IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))

                return TaskSchedule.tick(3)
            }
        })
    }

}