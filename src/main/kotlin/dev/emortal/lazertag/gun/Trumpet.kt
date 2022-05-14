package dev.emortal.lazertag.gun

import dev.emortal.lazertag.game.LazerTagGame
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.network.packet.client.ClientPacketsHandler.Play
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.Manager
import world.cepi.kstom.util.playSound
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

object Trumpet : Gun("Trumpet", Rarity.IMPOSSIBLE, { it.customModelData(1) }) {

    override val material: Material = Material.PHANTOM_MEMBRANE
    override val color: TextColor = NamedTextColor.YELLOW

    override val damage = 0f
    override val ammo = Integer.MAX_VALUE
    override val reloadTime = 0L
    override val cooldown = 60L

    override val sound = Sound.sound(Key.key("item.trumpet.doot"), Sound.Source.MASTER, 1f, 1f)

    override fun shoot(game: LazerTagGame, player: Player): HashMap<Player, Float> {
        game.playSound(sound, player.position)

        game.instance.getNearbyEntities(player.position, 8.0)
            .filter { it is Player && it.gameMode == GameMode.ADVENTURE && it != player }
            .forEach {
                val target = it as Player

                it.velocity = it.position.sub(player.position).asVec().normalize().mul(65.0).withY { 17.0 }

                game.damageMap.putIfAbsent(target, ConcurrentHashMap())
                game.damageMap[target]!![player]?.second?.cancel()

                val removalTask = Manager.scheduler.buildTask {
                    game.damageMap[target]?.remove(player)
                }.delay(Duration.ofSeconds(6)).schedule()

                game.damageMap[target]!![player] = Pair(game.damageMap[target]!![player]?.first ?: 0f, removalTask)
            }



        return HashMap()
    }

    override fun renderAmmo(player: Player, currentAmmo: Int, percentage: Float, reloading: Boolean) {

    }

}