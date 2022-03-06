package dev.emortal.lazertag.gun

import dev.emortal.lazertag.game.LazerTagGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.animal.BeeMeta
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.util.eyePosition
import world.cepi.kstom.util.playSound
import world.cepi.kstom.util.spread
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle

object BeeShotgun : ProjectileGun("Bee Keeper") {

    override val material: Material = Material.BEEHIVE
    override val color: TextColor = NamedTextColor.YELLOW

    override val damage = 1.25f
    override val numberOfBullets = 20
    override val spread = 0.12
    override val cooldown = 350L
    override val ammo = 6
    override val reloadTime = 1500L
    override val freshReload = false
    override val shootMidReload = true

    override val sound = Sound.sound(SoundEvent.ENTITY_BEE_HURT, Sound.Source.PLAYER, 1f, 1f)

    override fun projectileShot(game: LazerTagGame, player: Player): HashMap<Player, Float> {

        return HashMap()
    }

    override fun tick(game: LazerTagGame, projectile: Entity) {
        projectile.velocity = projectile.velocity.mul(1.02)
    }

    override fun collided(game: LazerTagGame, shooter: Player, projectile: Entity) {
        shooter.instance!!.showParticle(
            Particle.particle(
                type = ParticleType.EXPLOSION,
                count = 1,
                data = OffsetAndSpeed(0f, 0f, 0f, 0f),
            ),
            projectile.position.asVec()
        )
        shooter.instance!!.playSound(
            Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 0.25f, 1.5f),
            projectile.position
        )

        val boundingBox = projectile.boundingBox.expand(1.0, 1.0, 1.0)

        shooter.instance!!.players
            .filter { it.gameMode == GameMode.ADVENTURE }
            .filter { boundingBox.intersect(it.boundingBox) }
            .forEach { loopedPlayer ->
                if (loopedPlayer == shooter) return@forEach

                shooter.instance!!.playSound(
                    Sound.sound(SoundEvent.ENTITY_BEE_STING, Sound.Source.PLAYER, 1f, 1f),
                    projectile.position
                )

                game.damage(
                    shooter,
                    loopedPlayer,
                    false,
                    (damage / (projectile.getDistance(loopedPlayer) / 1.75).toFloat()).coerceAtMost(damage)
                )
            }
    }

    override fun createEntity(shooter: Player): Entity {
        val projectile = Entity(EntityType.BEE)

        val meta = projectile.entityMeta as BeeMeta
        meta.isBaby = true

        val velocity = shooter.position.direction().spread(spread).mul(30.0)
        projectile.velocity = velocity

        projectile.setBoundingBox(0.5, 0.5, 0.5)

        projectile.setNoGravity(true)
        projectile.setInstance(shooter.instance!!, shooter.eyePosition())

        return projectile
    }

}