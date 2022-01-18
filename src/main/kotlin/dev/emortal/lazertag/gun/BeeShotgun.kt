package dev.emortal.lazertag.gun

import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.metadata.animal.BeeMeta
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.util.eyePosition
import world.cepi.kstom.util.playSound
import world.cepi.kstom.util.spread
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle
import java.time.Duration

object BeeShotgun : ProjectileGun("Bee Keeper") {

    override val material: Material = Material.BEEHIVE
    override val color: TextColor = NamedTextColor.YELLOW

    override val damage = 3f
    override val numberOfBullets = 15
    override val spread = 0.07
    override val cooldown = 15
    override val ammo = 4
    override val reloadTime = 30
    override val freshReload = false

    override val sound = Sound.sound(SoundEvent.ENTITY_BEE_HURT, Sound.Source.PLAYER, 1f, 1f)

    override fun shoot(player: Player): HashMap<Player, Float> {
        repeat(numberOfBullets) {
            val projectile = Entity(EntityType.BEE)

            val meta = projectile.entityMeta as BeeMeta
            meta.isBaby = true

            val velocity = player.position.direction().spread(spread).mul(24.0)
            projectile.velocity = velocity

            projectile.boundingBox = projectile.boundingBox.expand(1.0, 1.0, 1.0)

            projectile.setNoGravity(true)
            projectile.setInstance(player.instance!!, player.eyePosition())

            projectile.setTag(playerUUIDTag, player.uuid.toString())
            projectile.setTag(gunIdTag, this.name)

            val tickTask = object : MinestomRunnable(repeat = Duration.ofMillis(50)) {
                override fun run() {
                    projectile.velocity = velocity
                }
            }

            entityTaskMap[projectile] = tickTask
        }

        val newAmmo = player.itemInMainHand.meta.getTag(ammoTag)!! - 1
        renderAmmo(player, newAmmo)
        player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
            meta.set(ammoTag, newAmmo)
        }

        return HashMap()
    }

    override fun collide(shooter: Player, projectile: Entity) {
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

        entityTaskMap[projectile]?.cancel()

        projectile.boundingBox = projectile.boundingBox.expand(1.0, 1.0, 1.0)

        shooter.instance!!.players
            .filter { it.gameMode == GameMode.ADVENTURE }
            .filter { projectile.boundingBox.intersect(it.boundingBox) }
            .forEach { loopedPlayer ->
                shooter.instance!!.playSound(
                    Sound.sound(SoundEvent.ENTITY_BEE_STING, Sound.Source.PLAYER, 1f, 1f),
                    projectile.position
                )

                loopedPlayer.scheduleNextTick {
                    loopedPlayer.damage(
                        DamageType.fromPlayer(shooter),
                        (damage / (projectile.getDistanceSquared(it) / 1.75).toFloat()).coerceAtMost(damage)
                    )
                }
            }

        projectile.remove()
    }

}