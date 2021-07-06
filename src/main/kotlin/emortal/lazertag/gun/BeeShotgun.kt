package emortal.lazertag.gun

import emortal.lazertag.utils.ParticleUtils.sendParticle
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.metadata.animal.BeeMeta
import net.minestom.server.item.Material
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import world.cepi.kstom.util.eyePosition
import world.cepi.kstom.util.spread
import java.util.concurrent.ThreadLocalRandom

object BeeShotgun : Gun("Bee Keeper", 5) {

    override val material = Material.BEEHIVE
    override val color: TextColor = NamedTextColor.YELLOW

    override val damage = 5f
    override val numberOfBullets = 7
    override val spread = 0.1
    override val cooldown = 700L
    override val ammo = 5
    override val reloadTime = 1500L

    override val sound = Sound.sound(SoundEvent.BEE_HURT, Sound.Source.PLAYER, 1f, 1f)

    override fun shoot(player: Player): HashMap<Player, Float> {
        repeat(numberOfBullets) {
            val projectile = Entity(EntityType.BEE)

            val meta = projectile.entityMeta as BeeMeta
            meta.isAngry = true
            meta.isBaby = true

            projectile.velocity = player.position.direction.spread(spread, ThreadLocalRandom.current()).multiply(24)

            projectile.setNoGravity(false)
            projectile.setGravity(0.0, 0.0)
            projectile.setInstance(player.instance!!, player.eyePosition())

            projectile.setTag(Tag.String("playerUUID"), player.uuid.toString())
            projectile.setTag(Tag.Integer("gunID"), this.id)
        }

        return HashMap()
    }

    override fun collide(player: Player, projectile: Entity) {
        player.instance!!.sendParticle(Particle.EXPLOSION, projectile.position, 0f, 0f, 0f, 1)
        player.instance!!.playSound(Sound.sound(SoundEvent.BEE_STING, Sound.Source.PLAYER, 1f, 1f))

        val boundingBox = projectile.boundingBox.expand(5.0, 5.0, 5.0)
        for (entity in player.instance!!.entities.filter { boundingBox.intersect(it.boundingBox) && it is Player && it.gameMode == GameMode.ADVENTURE }) {
            entity.scheduleNextTick {
                (entity as Player).damage(DamageType.fromPlayer(player),
                    damage / (projectile.getDistanceSquared(entity) / 1.75).toFloat()
                )
            }
        }

        projectile.remove()
    }

}