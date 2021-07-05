package emortal.lazertag.gun

import emortal.lazertag.utils.ParticleUtils.sendParticle
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.metadata.animal.BeeMeta
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import world.cepi.kstom.util.eyePosition

object BeeCannon : Gun("Rocket Launcher", 4) {

    override val damage = 80f
    override val cooldown = 5000L
    override val ammo = 1
    override val reloadTime = 3000L

    override val sound = Sound.sound(SoundEvent.BEE_HURT, Sound.Source.PLAYER, 1f, 1f)

    override fun shoot(player: Player): HashMap<Player, Float> {
        val damageMap = HashMap<Player, Float>()

        val projectile = Entity(EntityType.BEE)

        val meta = projectile.entityMeta as BeeMeta
        meta.isAngry = true

        projectile.velocity = player.position.direction.multiply(24)

        projectile.setNoGravity(false)
        projectile.setGravity(0.0, 0.0)
        projectile.setInstance(player.instance!!, player.eyePosition())

        projectile.setTag(Tag.String("playerUUID"), player.uuid.toString())
        projectile.setTag(Tag.Integer("gunID"), this.id)

        return damageMap
    }

    override fun collide(player: Player, projectile: Entity) {
        player.instance!!.sendParticle(Particle.EXPLOSION_EMITTER, projectile.position, 0f, 0f, 0f, 1)
        player.instance!!.playSound(Sound.sound(SoundEvent.GENERIC_EXPLODE, Sound.Source.PLAYER, 1f, 1f))

        val boundingBox = projectile.boundingBox.expand(8.0, 8.0, 8.0)

        player.instance!!.entities
            .filterIsInstance<Player>()
            .filter { it.gameMode == GameMode.ADVENTURE }
            .filter { boundingBox.intersect(it.boundingBox) }
            .forEach { loopedPlayer ->
                loopedPlayer.scheduleNextTick {
                    loopedPlayer.damage(
                        DamageType.fromPlayer(player),
                        damage / (projectile.getDistanceSquared(loopedPlayer) / 1.75).toFloat()
                    )
                }
            }

        projectile.remove()
    }

}