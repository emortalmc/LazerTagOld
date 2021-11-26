package emortal.lazertag.gun

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.arrow.SpectralArrowMeta
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.util.eyePosition
import world.cepi.kstom.util.playSound
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle

object Crossbow : Gun("Crossbow") {

    override val material: Material = Material.BOW
    override val color: TextColor = NamedTextColor.GOLD

    override val damage = 999f
    override val ammo = 1
    override val reloadTime = 500L
    override val cooldown = reloadTime

    override val sound = Sound.sound(SoundEvent.ITEM_CROSSBOW_SHOOT, Sound.Source.PLAYER, 1f, 1f)

    override fun shoot(player: Player): HashMap<Player, Float> {
        val damageMap = HashMap<Player, Float>()

        val projectile = Entity(EntityType.SPECTRAL_ARROW)
        val projectilemeta = projectile.entityMeta as SpectralArrowMeta

        val velocity = player.position.direction().mul(50.0)
        projectile.velocity = velocity

        projectile.setInstance(player.instance!!, player.eyePosition())

        projectile.setTag(playerUUIDTag, player.uuid.toString())
        projectile.setTag(gunIdTag, this.name)

        val newAmmo = player.itemInMainHand.meta.getTag(ammoTag)!! - 1
        renderAmmo(player, newAmmo)
        player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
            meta.set(ammoTag, newAmmo)
        }

        return damageMap
    }

    override fun collide(player: Player, projectile: Entity) {
        player.instance!!.showParticle(
            Particle.particle(
                type = ParticleType.EXPLOSION,
                count = 1,
                data = OffsetAndSpeed(0f, 0f, 0f, 0f),
            ),
            projectile.position.asVec()
        )
        player.instance!!.playSound(
            Sound.sound(SoundEvent.ENTITY_ARROW_HIT, Sound.Source.PLAYER, 1f, 1f),
            projectile.position
        )

        projectile.remove()
    }

}