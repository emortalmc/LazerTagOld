package emortal.lazertag.gun

import emortal.immortal.particle.ParticleUtils
import emortal.immortal.particle.shapes.sendParticle
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
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.util.component1
import world.cepi.kstom.util.component2
import world.cepi.kstom.util.component3
import world.cepi.kstom.util.eyePosition
import kotlin.math.min

object BeeCannon : Gun("Bee Launcher") {

    override val material: Material = Material.HONEYCOMB
    override val color: TextColor = NamedTextColor.YELLOW

    override val damage = 100f
    override val cooldown = 3000L
    override val ammo = 1
    override val reloadTime = 2300L

    override val sound = Sound.sound(SoundEvent.ENTITY_BEE_HURT, Sound.Source.PLAYER, 1f, 1f)

    override fun shoot(player: Player): HashMap<Player, Float> {
        val damageMap = HashMap<Player, Float>()

        val projectile = Entity(EntityType.BEE)

        val (x, y, z) = projectile.position

        val velocity = player.position.direction().mul(30.0)
        projectile.velocity = velocity

        projectile.setNoGravity(true)
        projectile.setInstance(player.instance!!, player.eyePosition())

        projectile.setTag(playerUUIDTag, player.uuid.toString())
        projectile.setTag(gunIdTag, this.name)

        val tickTask = Manager.scheduler.buildTask {
            projectile.velocity = velocity
            player.instance!!.sendParticle(ParticleUtils.particle(Particle.LARGE_SMOKE, x, y, z,count = 1))
        }.repeat(1, TimeUnit.SERVER_TICK).schedule()

        projectile.setTag(Tag.Integer("taskID"), tickTask.id)

        val newAmmo = player.itemInMainHand.meta.getTag(ammoTag)!! - 1
        renderAmmo(player, newAmmo)
        player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
            meta.set(ammoTag, newAmmo)
        }

        return damageMap
    }

    override fun collide(player: Player, projectile: Entity) {
        val (x, y, z) = projectile.position

        projectile.viewers.sendParticle(ParticleUtils.particle(Particle.EXPLOSION_EMITTER, x, y, z))
        player.instance!!.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.PLAYER, 1f, 1f))

        val boundingBox = projectile.boundingBox.expand(8.0, 8.0, 8.0)

        Manager.scheduler.getTask(projectile.getTag(Tag.Integer("taskID"))!!).cancel()

        player.instance!!.entities
            .filterIsInstance<Player>()
            .filter { it.gameMode == GameMode.ADVENTURE }
            .filter { boundingBox.intersect(it.boundingBox) }
            .forEach { loopedPlayer ->
                loopedPlayer.scheduleNextTick {
                    loopedPlayer.damage(
                        DamageType.fromPlayer(player),
                        min(damage / (projectile.getDistanceSquared(loopedPlayer) / 1.75).toFloat(), damage)
                    )
                }
            }

        projectile.remove()
    }

}