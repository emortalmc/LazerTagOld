package dev.emortal.lazertag.gun

import dev.emortal.lazertag.game.LazerTagGame
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.FallingBlockMeta
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ExplosionPacket
import world.cepi.kstom.util.component1
import world.cepi.kstom.util.component2
import world.cepi.kstom.util.component3
import world.cepi.kstom.util.eyePosition
import java.util.concurrent.ConcurrentHashMap

object BlockChucker : ProjectileGun("Block Chucker", Rarity.RARE) {

    override val material: Material = Material.SPAWNER
    override val color: TextColor = NamedTextColor.GREEN

    override val damage = 10f
    override val ammo = 5
    override val reloadTime: Int = 3000
    override val cooldown: Int = 500

    override val sound = null

    override fun projectileShot(game: LazerTagGame, player: Player): ConcurrentHashMap<Player, Float> {
        return ConcurrentHashMap()
    }

    override fun collided(game: LazerTagGame, shooter: Player, projectile: Entity) {
        val (x, y, z) = projectile.position
        shooter.instance!!.sendGroupedPacket(ExplosionPacket(x.toFloat(), y.toFloat(), z.toFloat(), 3f, ByteArray(0), 0f, 0f, 0f))

        shooter.instance!!.players
            .filter { it.gameMode == GameMode.ADVENTURE }
            .filter { it.getDistance(projectile) < 7 }
            .forEach { loopedPlayer ->
                loopedPlayer.velocity =
                    loopedPlayer.position.sub(projectile.position.sub(.0, .5, .0)).asVec().normalize().mul(30.0)

                game.damage(
                    shooter, loopedPlayer, false, (damage / (projectile.getDistance(loopedPlayer) / 1.75).toFloat())
                        .coerceAtMost(damage)
                )
            }
    }

    override fun createEntity(shooter: Player): Entity {
        val projectile = Entity(EntityType.FALLING_BLOCK)
        val meta = projectile.entityMeta as FallingBlockMeta
        meta.block = Block.values().random()

        projectile.velocity = shooter.position.direction().mul(50.0)

        shooter.playSound(
            Sound.sound(
                Key.key("minecraft:block.${meta.block.name().replace("minecraft:", "")}.break"),
                Sound.Source.MASTER,
                1f,
                1f
            )
        )

        projectile.setInstance(shooter.instance!!, shooter.eyePosition())

        return projectile
    }

}