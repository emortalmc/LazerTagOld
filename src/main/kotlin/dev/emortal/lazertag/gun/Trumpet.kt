package dev.emortal.lazertag.gun

import dev.emortal.lazertag.game.LazerTagGame
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import world.cepi.kstom.Manager
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

object Trumpet : Gun("Trumpet", Rarity.IMPOSSIBLE, { it.customModelData(1) }) {

    override val material: Material = Material.PHANTOM_MEMBRANE
    override val color: TextColor = NamedTextColor.YELLOW

    override val damage = 0f
    override val ammo = Integer.MAX_VALUE
    override val reloadTime: Int = 0
    override val cooldown: Int = 60

    override val sound = Sound.sound(Key.key("item.trumpet.doot"), Sound.Source.MASTER, 1f, 1f)

    override fun shoot(game: LazerTagGame, player: Player): ConcurrentHashMap<Player, Float> {
        game.playSound(sound, player.position)

        val instance = game.instance

        instance.getNearbyEntities(player.position, 8.0)
            .filter { it is Player && it.gameMode == GameMode.ADVENTURE && it != player }
            .forEach {
                val target = it as Player

                it.velocity = it.position.sub(player.position).asVec().normalize().mul(65.0).withY(17.0)

                game.damageMap.putIfAbsent(target.uuid, ConcurrentHashMap())
                game.damageMap[target.uuid]!![player.uuid]?.second?.cancel()

                val removalTask = Manager.scheduler.buildTask {
                    game.damageMap[target.uuid]?.remove(player.uuid)
                }.delay(Duration.ofSeconds(6)).schedule()

                game.damageMap[target.uuid]!![player.uuid] = Pair(game.damageMap[target.uuid]!![player.uuid]?.first ?: 0f, removalTask)
            }

        return ConcurrentHashMap()
    }

    override fun renderAmmo(player: Player, currentAmmo: Int, percentage: Float, reloading: Boolean) {

    }

}