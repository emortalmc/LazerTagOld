package dev.emortal.lazertag.gun

import dev.emortal.lazertag.game.LazerTagGame
import dev.emortal.lazertag.utils.showFirework
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.util.eyePosition
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle
import java.awt.Color
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

object Celebration : ProjectileGun("Celebration", Rarity.RARE) {

    override val material: Material = Material.FIREWORK_ROCKET
    override val color: TextColor = NamedTextColor.LIGHT_PURPLE

    override val damage = 7f
    override val ammo = 10
    override val reloadTime = 3500L
    override val cooldown = 300L

    override val boundingBoxExpand = Vec(0.2, 0.2, 0.2)

    override val sound = Sound.sound(SoundEvent.ENTITY_FIREWORK_ROCKET_LAUNCH, Sound.Source.PLAYER, 1f, 1f)

    override fun projectileShot(game: LazerTagGame, player: Player): ConcurrentHashMap<Player, Float> {
        return ConcurrentHashMap()
    }

    override fun tick(game: LazerTagGame, projectile: Entity, shooter: Player) {
        projectile.velocity = projectile.velocity.mul(1.02)

        game.showParticle(
            Particle.particle(
                type = ParticleType.CLOUD,
                count = 1,
                data = OffsetAndSpeed()
            ),
            projectile.position.asVec()
        )
    }

    override fun collided(game: LazerTagGame, shooter: Player, projectile: Entity) {

        val random = ThreadLocalRandom.current()
        val effects = mutableListOf(
            FireworkEffect(
                random.nextBoolean(),
                false,
                FireworkEffectType.values().random(),
                listOf(net.minestom.server.color.Color(Color.HSBtoRGB(random.nextFloat(), 1f, 1f))),
                listOf(net.minestom.server.color.Color(Color.HSBtoRGB(random.nextFloat(), 1f, 1f)))
            )
        )
        shooter.instance!!.showFirework(shooter.instance!!, projectile.position, effects)

        shooter.instance!!.players
            .filter { it.gameMode == GameMode.ADVENTURE }
            .filter { it.getDistanceSquared(projectile) < 5 * 5 }
            .forEach { loopedPlayer ->
                loopedPlayer.velocity =
                    loopedPlayer.position.sub(projectile.position.sub(.0, .5, .0)).asVec().normalize().mul(30.0)

                game.damage(
                    shooter,
                    loopedPlayer,
                    false,
                    if (loopedPlayer == shooter) 2f else (damage / (projectile.getDistance(loopedPlayer) / 3.0).toFloat())
                        .coerceAtMost(damage)
                )
            }

        projectile.remove()
    }

    override fun createEntity(shooter: Player): Entity {
        val projectile = Entity(EntityType.FIREWORK_ROCKET)
        val velocity = shooter.position.direction().mul(40.0)

        projectile.velocity = velocity

        projectile.setNoGravity(true)
        projectile.setInstance(shooter.instance!!, shooter.eyePosition())
        projectile.scheduleRemove(Duration.ofSeconds(10))

        return projectile
    }

}