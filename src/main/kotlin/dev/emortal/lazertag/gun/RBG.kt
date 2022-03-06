package dev.emortal.lazertag.gun

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.lazertag.game.LazerTagGame
import dev.emortal.lazertag.utils.getPointsInSphere
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.util.eyePosition
import world.cepi.kstom.util.playSound
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle

object RBG : ProjectileGun("RBG", Rarity.IMPOSSIBLE) {

    override val material: Material = Material.TNT
    override val color: TextColor = NamedTextColor.RED

    override val damage = 1f
    override val ammo = 50
    override val reloadTime = 2000L
    override val cooldown = 80L
    override val burstInterval = 30L
    override val burstAmount = 5

    override val sound = Sound.sound(SoundEvent.ENTITY_BEE_HURT, Sound.Source.PLAYER, 1f, 1f)

    override fun projectileShot(game: LazerTagGame, player: Player): HashMap<Player, Float> {
        val damageMap = HashMap<Player, Float>()

        return damageMap
    }

    override fun tick(game: LazerTagGame, projectile: Entity) {
        projectile.velocity = projectile.velocity.mul(1.02)

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
        shooter.instance!!.showParticle(
            Particle.particle(
                type = ParticleType.EXPLOSION_EMITTER,
                count = 1,
                data = OffsetAndSpeed(),
            ),
            projectile.position.asVec()
        )

        shooter.instance!!.playSound(
            Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.PLAYER, 1f, 1f),
            projectile.position
        )

        shooter.instance!!.players
            .filter { it.gameMode == GameMode.ADVENTURE }
            .filter { it.getDistanceSquared(projectile) < 8 * 8 }
            .forEach { loopedPlayer ->
                loopedPlayer.velocity =
                    loopedPlayer.position.sub(projectile.position.sub(.0, .5, .0)).asVec().normalize().mul(40.0)

                game.damage(
                    shooter,
                    loopedPlayer,
                    false,
                    if (loopedPlayer == shooter) 0f else (BeeBlaster.damage / (projectile.getDistance(loopedPlayer)).toFloat())
                        .coerceAtMost(BeeBlaster.damage)
                )
            }

        if ((shooter.game!! as LazerTagGame).destructible) {
            val batch = AbsoluteBlockBatch()
            getPointsInSphere(projectile.position, 3).forEach {
                batch.setBlock(it, Block.AIR)
            }
            batch.apply(shooter.instance!!) {}
        }

        projectile.remove()
    }

    override fun createEntity(shooter: Player): Entity {
        val projectile = Entity(EntityType.BEE)
        val velocity = shooter.position.direction().mul(25.0)

        projectile.velocity = velocity

        projectile.setNoGravity(true)
        projectile.setInstance(shooter.instance!!, shooter.eyePosition())

        return projectile
    }

}