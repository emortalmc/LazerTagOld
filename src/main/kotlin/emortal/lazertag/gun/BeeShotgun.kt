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
import net.minestom.server.entity.metadata.animal.BeeMeta
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.Material
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.util.*
import kotlin.math.min

object BeeShotgun : Gun("Bee Keeper") {

    override val material: Material = Material.BEEHIVE
    override val color: TextColor = NamedTextColor.YELLOW

    override val damage = 6f
    override val numberOfBullets = 7
    override val spread = 0.2
    override val cooldown = 700L
    override val ammo = 4
    override val reloadTime = 1500L

    override val sound = Sound.sound(SoundEvent.ENTITY_BEE_HURT, Sound.Source.PLAYER, 1f, 1f)

    override fun shoot(player: Player): HashMap<Player, Float> {
        repeat(numberOfBullets) {
            val projectile = Entity(EntityType.BEE)

            val meta = projectile.entityMeta as BeeMeta
            meta.isBaby = true

            val velocity = player.position.direction().spread(spread).mul(24.0)
            projectile.velocity = velocity

            projectile.setNoGravity(true)
            projectile.setInstance(player.instance!!, player.eyePosition())

            projectile.setTag(playerUUIDTag, player.uuid.toString())
            projectile.setTag(gunIdTag, this.name)

            val tickTask = Manager.scheduler.buildTask {
                projectile.velocity = velocity
                //player.instance!!.sendParticle(ParticleUtils.particle(Particle.LARGE_SMOKE, projectile.position, Vec.ZERO, 1, 0f))
            }.repeat(1, TimeUnit.SERVER_TICK).schedule()

            projectile.setTag(Tag.Integer("taskID"), tickTask.id)
        }

        val newAmmo = player.itemInMainHand.meta.getTag(ammoTag)!! - 1
        renderAmmo(player, newAmmo)
        player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
            meta.set(ammoTag, newAmmo)
        }

        return HashMap()
    }

    override fun collide(player: Player, projectile: Entity) {
        val (x, y, z) = projectile.position

        projectile.viewers.sendParticle(ParticleUtils.particle(Particle.EXPLOSION, x, y, z))
        player.instance!!.playSound(Sound.sound(SoundEvent.ENTITY_BEE_STING, Sound.Source.PLAYER, 1f, 1f))

        Manager.scheduler.getTask(projectile.getTag(Tag.Integer("taskID"))!!).cancel()

        val boundingBox = projectile.boundingBox.expand(5.0, 5.0, 5.0)
        for (entity in player.instance!!.entities.filter { boundingBox.intersect(it.boundingBox) && it is Player && it.gameMode == GameMode.ADVENTURE }) {
            entity.scheduleNextTick {
                (entity as Player).damage(DamageType.fromPlayer(player),
                    min(damage / (projectile.getDistanceSquared(entity) / 1.75).toFloat(), damage)
                )
            }
        }

        projectile.remove()
    }

}