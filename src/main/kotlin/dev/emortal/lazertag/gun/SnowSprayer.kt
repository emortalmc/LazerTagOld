package dev.emortal.lazertag.gun
/*
import dev.emortal.lazertag.game.LazerTagGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
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

    override val damage = 2f
    override val ammo = 30
    override val reloadTime = 2500L
    override val cooldown = 350L

    override val burstAmount = 5
    override val burstInterval = 50L

    override val spread = 0.025

    override val sound = Sound.sound(SoundEvent.ENTITY_SNOWBALL_THROW, Sound.Source.MASTER, 1f, 1.5f)

    override fun projectileShot(game: LazerTagGame, player: Player): HashMap<Player, Float> {
        val damageMap = HashMap<Player, Float>()

        return damageMap
    }

    override fun collided(game: LazerTagGame, shooter: Player, projectile: Entity) {
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
    }

    override fun collidedWithEntity(
        game: LazerTagGame,
        shooter: Player,
        projectile: Entity,
        hitPlayers: Collection<Player>
    ) {
        hitPlayers.forEach { loopedPlayer ->
            game.damage(shooter, loopedPlayer, false, damage)
        }
    }

    override fun createEntity(shooter: Player): Entity {
        val projectile = Entity(EntityType.SNOWBALL)
        val velocity = shooter.position.direction().mul(50.0)
        projectile.velocity = velocity
        projectile.setBoundingBox(0.5, 0.5, 0.5)

        projectile.setInstance(shooter.instance!!, shooter.eyePosition())
        projectile.scheduleRemove(Duration.ofSeconds(10))

        return projectile
    }

}*/