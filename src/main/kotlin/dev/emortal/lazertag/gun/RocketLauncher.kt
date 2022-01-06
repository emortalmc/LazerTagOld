package dev.emortal.lazertag.gun

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.util.eyePosition
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle
import kotlin.math.min

object RocketLauncher : ProjectileGun("Bee Gun") {

    override val material: Material = Material.HONEYCOMB
    override val color: TextColor = NamedTextColor.YELLOW

    override val damage = 100f
    override val ammo = 1
    override val reloadTime = 50
    override val cooldown = reloadTime

    override val sound = Sound.sound(SoundEvent.ENTITY_BEE_HURT, Sound.Source.PLAYER, 1f, 1f)

    override fun shoot(player: Player): HashMap<Player, Float> {
        val damageMap = HashMap<Player, Float>()

        val projectile = Entity(EntityType.BEE)


        val velocity = player.position.direction().mul(30.0)
        projectile.velocity = velocity

        projectile.setNoGravity(true)
        projectile.setInstance(player.instance!!, player.eyePosition())

        projectile.setTag(playerUUIDTag, player.uuid.toString())
        projectile.setTag(gunIdTag, this.name)

        val tickTask = Manager.scheduler.buildTask {
            projectile.velocity = velocity
            player.instance!!.showParticle(
                Particle.particle(
                    type = ParticleType.LARGE_SMOKE,
                    count = 1,
                    data = OffsetAndSpeed(0.2f, 0.2f, 0.2f, 0.01f),
                ),
                projectile.position.asVec()
            )
        }.repeat(1, TimeUnit.SERVER_TICK).schedule()

        entityTaskMap[projectile] = tickTask

        val newAmmo = player.itemInMainHand.meta.getTag(ammoTag)!! - 1
        renderAmmo(player, newAmmo)
        player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
            meta.set(ammoTag, newAmmo)
        }

        return damageMap
    }

    override fun collide(shooter: Player, projectile: Entity) {
        shooter.instance!!.showParticle(
            Particle.particle(
                type = ParticleType.EXPLOSION_EMITTER,
                count = 1,
                data = OffsetAndSpeed(0f, 0f, 0f, 0f),
            ),
            projectile.position.asVec()
        )
        shooter.instance!!.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.PLAYER, 1f, 1f))

        val boundingBox = projectile.boundingBox.expand(8.0, 8.0, 8.0)

        entityTaskMap[projectile]?.cancel()

        shooter.instance!!.entities
            .filterIsInstance<Player>()
            .filter { it.gameMode == GameMode.ADVENTURE }
            .filter { boundingBox.intersect(it.boundingBox) }
            .forEach { loopedPlayer ->
                loopedPlayer.velocity =
                    loopedPlayer.position.sub(projectile.position.sub(.0, .5, .0)).asVec().normalize().mul(60.0)

                loopedPlayer.scheduleNextTick {
                    loopedPlayer.damage(
                        DamageType.fromPlayer(shooter),
                        min(
                            damage / (projectile.getDistance(loopedPlayer) / 1.75).toFloat(),
                            damage
                        ).coerceAtMost(if (loopedPlayer == shooter) 5f else 20f)
                    )
                }
            }

        projectile.remove()
    }

}