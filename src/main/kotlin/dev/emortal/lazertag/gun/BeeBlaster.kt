package dev.emortal.lazertag.gun

import dev.emortal.lazertag.entity.NoDragEntityProjectile
import dev.emortal.lazertag.game.LazerTagGame
import dev.emortal.lazertag.game.LazerTagPlayerHelper.hasSpawnProtection
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ExplosionPacket
import net.minestom.server.sound.SoundEvent
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle
import java.time.Duration

object BeeBlaster : ProjectileGun("Bee Blaster", Rarity.RARE) {

    override val material: Material = Material.HONEYCOMB
    override val color: TextColor = NamedTextColor.YELLOW

    override val damage = 70f
    override val ammo = 1
    override val reloadTime: Int = 2000
    override val cooldown: Int = reloadTime

    override val sound = Sound.sound(SoundEvent.ENTITY_BEE_HURT, Sound.Source.PLAYER, 1f, 1f)

    override fun projectileShot(game: LazerTagGame, player: Player): Map<Player, Float> {
        return emptyMap()
    }

    override fun tick(game: LazerTagGame, projectile: Entity, shooter: Player) {
        game.showParticle(
            Particle.particle(
                type = ParticleType.LARGE_SMOKE,
                count = 1,
                data = OffsetAndSpeed()
            ),
            projectile.position.asVec()
        )
    }

    override fun collided(game: LazerTagGame, shooter: Player, projectile: Entity) {
        shooter.instance!!.sendGroupedPacket(ExplosionPacket(projectile.position.x, projectile.position.y, projectile.position.z, 3f, ByteArray(0), 0f, 0f, 0f))

        shooter.instance!!.players
            .filter { it.gameMode == GameMode.ADVENTURE && !it.hasSpawnProtection }
            .filter { it.getDistanceSquared(projectile) < 8 * 8 }
            .forEach { loopedPlayer ->
                loopedPlayer.velocity =
                    loopedPlayer.position.sub(projectile.position.sub(.0, .5, .0)).asVec().normalize().mul(60.0)

                game.damage(
                    shooter,
                    loopedPlayer,
                    false,
                    if (loopedPlayer == shooter) 5f else (damage / (projectile.getDistance(loopedPlayer)).toFloat())
                        .coerceAtMost(damage)
                )
            }

        projectile.remove()
    }

    override fun createEntity(shooter: Player): Entity {
        val projectile = NoDragEntityProjectile(shooter, EntityType.BEE)
        val velocity = shooter.position.direction().mul(25.0)

        projectile.velocity = velocity

        projectile.setNoGravity(true)
        projectile.setInstance(shooter.instance!!, shooter.position.add(0.0, shooter.eyeHeight, 0.0))
        projectile.scheduleRemove(Duration.ofSeconds(10))

        return projectile
    }

}