package dev.emortal.lazertag.gun

import dev.emortal.lazertag.game.LazerTagGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.util.eyePosition
import world.cepi.kstom.util.playSound
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle

object SnowSprayer : ProjectileGun("Snow Sprayer") {

    override val material: Material = Material.SNOW_BLOCK
    override val color: TextColor = NamedTextColor.WHITE

    override val damage: Float = 2f
    override val ammo: Int = 40
    override val reloadTime: Int = 3 * 20
    override val cooldown: Int = 7

    override val burstAmount: Int = 5
    override val burstInterval = 1

    override val sound = Sound.sound(SoundEvent.ENTITY_SNOWBALL_THROW, Sound.Source.MASTER, 1f, 1.5f)

    override fun projectileShot(game: LazerTagGame, player: Player): HashMap<Player, Float> {
        val damageMap = HashMap<Player, Float>()

        return damageMap
    }

    override fun collided(shooter: Player, projectile: Entity) {
        shooter.instance!!.showParticle(
            Particle.particle(
                type = ParticleType.SNOWFLAKE,
                count = 5,
                data = OffsetAndSpeed(0.2f, 0.2f, 0.2f, 0f),
            ),
            projectile.position.asVec()
        )
        shooter.instance!!.playSound(
            Sound.sound(SoundEvent.BLOCK_SNOW_BREAK, Sound.Source.PLAYER, 0.75f, 2f),
            projectile.position
        )

        shooter.instance!!.players
            .filter { it.gameMode == GameMode.ADVENTURE }
            .filter { projectile.boundingBox.intersect(it) }
            .forEach { loopedPlayer ->
                loopedPlayer.scheduleNextTick {
                    loopedPlayer.damage(DamageType.fromPlayer(shooter), damage)
                }
            }
    }

    override fun createEntity(shooter: Player): Entity {
        val projectile = Entity(EntityType.SNOWBALL)
        val velocity = shooter.position.direction().mul(50.0)
        projectile.velocity = velocity
        projectile.setBoundingBox(0.5, 0.5, 0.5)

        projectile.setInstance(shooter.instance!!, shooter.eyePosition())

        return projectile
    }

}