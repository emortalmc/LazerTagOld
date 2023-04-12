package dev.emortal.lazertag.gun

import dev.emortal.lazertag.game.LazerTagGame
import dev.emortal.lazertag.game.LazerTagPlayerHelper.hasSpawnProtection
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.*
import net.minestom.server.entity.metadata.monster.CreeperMeta
import net.minestom.server.entity.metadata.water.fish.PufferfishMeta
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ExplosionPacket
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

object MobLobber : ProjectileGun("Mob Lobber", Rarity.RARE) {

    val mobList = listOf(
        EntityType.SHEEP,
        EntityType.PIG,
        EntityType.CHICKEN,
        EntityType.COW,
        EntityType.HORSE,
        EntityType.OCELOT,
        EntityType.LLAMA,
        EntityType.PANDA,
        EntityType.WITCH,
        EntityType.VILLAGER,
        EntityType.COD,
        EntityType.PUFFERFISH,
        EntityType.TURTLE,
        EntityType.ZOMBIE,
        EntityType.SKELETON,
        EntityType.CREEPER,
        EntityType.ENDER_DRAGON
    )

    override val material: Material = Material.SPAWNER
    override val color: TextColor = NamedTextColor.GREEN

    override val damage = 10f
    override val ammo = 5
    override val reloadTime: Int = 3000
    override val cooldown: Int = 500

    override val sound = null

    override fun projectileShot(game: LazerTagGame, player: Player): Map<Player, Float> {
        return emptyMap()
    }

    override fun collided(game: LazerTagGame, shooter: Player, projectile: Entity) {
        shooter.instance!!.sendGroupedPacket(ExplosionPacket(projectile.position.x(), projectile.position.y(), projectile.position.z(), 3f, ByteArray(0), 0f, 0f, 0f))

        shooter.instance!!.players
            .filter { it.gameMode == GameMode.ADVENTURE && !it.hasSpawnProtection }
            .filter { it.getDistanceSquared(projectile) < 7*7 }
            .forEach { loopedPlayer ->
                loopedPlayer.velocity =
                    loopedPlayer.position.sub(projectile.position.sub(.0, .5, .0)).asVec().normalize().mul(30.0)

                game.damage(
                    shooter, loopedPlayer, false, if (loopedPlayer == shooter) 2.5f else (damage / (projectile.getDistance(loopedPlayer) / 1.75).toFloat())
                        .coerceAtMost(damage)
                )
            }
    }

    override fun createEntity(shooter: Player): Entity {
        val entityType = mobList.random()

        val projectile = EntityProjectile(shooter, entityType)
        projectile.setBoundingBox(1.2, 1.2, 1.2)
        val velocity = shooter.position.direction().mul(50.0)
        projectile.velocity = velocity
        projectile.setBoundingBox(1.5, 1.5, 1.5)

        when (entityType) {
            EntityType.CREEPER -> {
                val entityMeta = projectile.entityMeta as CreeperMeta
                entityMeta.isCharged = ThreadLocalRandom.current().nextBoolean()
                entityMeta.isIgnited = true
            }
            EntityType.PUFFERFISH -> {
                val entityMeta = projectile.entityMeta as PufferfishMeta
                entityMeta.state = PufferfishMeta.State.FULLY_PUFFED
            }
            else -> {

            }
        }

        shooter.playSound(
            Sound.sound(
                Key.key("minecraft:entity.${entityType.name().replace("minecraft:", "")}.hurt"),
                Sound.Source.NEUTRAL,
                1f,
                1f
            )
        )

        projectile.setInstance(shooter.instance!!, shooter.position.add(0.0, shooter.eyeHeight, 0.0))
        projectile.scheduleRemove(Duration.ofSeconds(10))

        return projectile
    }

}