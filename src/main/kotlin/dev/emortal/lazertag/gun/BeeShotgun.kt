package dev.emortal.lazertag.gun

import dev.emortal.lazertag.entity.NoDragEntityProjectile
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
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

object BeeShotgun : ProjectileGun("Bee Keeper") {

    override val material: Material = Material.BEEHIVE
    override val color: TextColor = NamedTextColor.YELLOW

    override val damage = 1.0f
    override val numberOfBullets = 20
    override val spread = 0.09
    override val cooldown: Int = 400
    override val ammo = 6
    override val reloadTime: Int = 1600
    override val freshReload = true
    override val shootMidReload = false

    override val sound = Sound.sound(SoundEvent.ENTITY_BEE_HURT, Sound.Source.PLAYER, 1f, 1f)

    override fun projectileShot(game: LazerTagGame, player: Player): Map<Player, Float> {
        return emptyMap()
    }

    override fun tick(game: LazerTagGame, projectile: Entity, shooter: Player) {
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
            .filter { boundingBox.intersectEntity(projectile.position, it) }
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
        val projectile = NoDragEntityProjectile(shooter, EntityType.BEE)

        val meta = projectile.entityMeta as BeeMeta
        meta.isBaby = true

        val random = ThreadLocalRandom.current()
        val lookDir = shooter.position.direction()
        var velocity = lookDir
            .mul(40.0)
        if (spread > 0.0) {
            velocity = velocity.rotateAroundAxis(lookDir.rotateAroundZ(Math.PI / 2), random.nextDouble(-spread, spread))
                .rotateAroundAxis(lookDir, random.nextDouble(Math.PI * 2))
        }

        projectile.velocity = velocity

        projectile.setBoundingBox(0.5, 0.5, 0.5)

        projectile.setNoGravity(true)
        projectile.setInstance(shooter.instance!!, shooter.position.add(0.0, shooter.eyeHeight, 0.0))
        projectile.scheduleRemove(Duration.ofSeconds(10))

        return projectile
    }

}