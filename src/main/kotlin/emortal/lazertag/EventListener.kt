package emortal.lazertag

import emortal.lazertag.gun.Gun
import emortal.lazertag.items.ItemManager
import emortal.lazertag.raycast.RayCast
import emortal.lazertag.utils.MinestomRunnable
import emortal.lazertag.utils.ParticleUtils
import emortal.lazertag.utils.PlayerUtils.eyePosition
import emortal.lazertag.utils.PlayerUtils.playSound
import emortal.lazertag.utils.RandomUtils
import emortal.lazertag.utils.RandomUtils.spread
import io.github.bloepiloepi.particles.shapes.ParticleShape
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.color.Color
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.damage.EntityDamage
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.raycast.HitType
import java.util.concurrent.ThreadLocalRandom

class EventListener {

    companion object {

        fun init(extension: Extension) {
            val eventNode = extension.eventNode

            eventNode.addListener(PlayerUseItemEvent::class.java) { e: PlayerUseItemEvent ->
                run {
                    if (e.hand != Player.Hand.MAIN) return@addListener

                    val eyePos = e.player.eyePosition()
                    val instance = e.player.instance!!
                    val eyeDir = eyePos.direction
                    val threadLocalRandom = ThreadLocalRandom.current()

                    val heldGun = Gun.registeredMap[e.player.inventory.itemInMainHand.meta.customModelData]!!

                    e.player.itemInMainHand = heldGun.itemBuilder.meta { meta: ItemMetaBuilder ->
                        meta.damage(e.player.itemInMainHand.meta.damage + 1)
                    }.build()

                    instance.playSound(Sound.sound(SoundEvent.BLAZE_HURT, Sound.Source.PLAYER, 2f, 1f), eyePos)

                    /*for (i in 0..20) {
                        val newDir = eyeDir.spread(0.15)

                        PlayerUtils.sendParticle(e.player, Particle.FLAME, eyePos.clone().subtract(0.0, 0.25, 0.0), newDir.x.toFloat(), newDir.y.toFloat(), newDir.z.toFloat(), 0, 1f)
                    }*/

                    var damage = 0f
                    var hitEntity: LivingEntity? = null

                    repeat(heldGun.numberOfBullets - 1) {
                        val direction = eyeDir.spread(heldGun.spread).normalize()

                        val raycast = RayCast.castRay(
                            instance,
                            e.player,
                            eyePos.toVector(),
                            direction,
                            25.0,
                            0.5
                        )
                        val lastPos = raycast.finalPosition.toPosition()


                        if (raycast.hitType == HitType.ENTITY) {
                            hitEntity = raycast.hitEntity!!

                            damage += heldGun.damage

                            val shapeOptions = ParticleUtils.getColouredShapeOptions(Color(255, 0, 0), Color(20, 20, 20), 1.1f)
                            ParticleShape.line(raycast.finalPosition.add(direction.multiply(6)).toPosition(), lastPos)
                                .iterator(shapeOptions).draw(instance, RandomUtils.ZERO_POS)
                        } else {
                            val shapeOptions = ParticleUtils.getColouredShapeOptions(Color(100, 100, 100), Color(50, 50, 50), 0.2f)
                            ParticleShape.line(eyePos, lastPos)
                                .iterator(shapeOptions).draw(instance, RandomUtils.ZERO_POS)
                        }


                    }

                    if (hitEntity != null) {
                        e.player.playSound(Sound.sound(SoundEvent.PLAYER_HURT, Sound.Source.PLAYER, 1f, 1f))

                        hitEntity!!.damage(DamageType.fromPlayer(e.player), damage)
                    }


                }
            }

            eventNode.addListener(PlayerSpawnEvent::class.java) { e: PlayerSpawnEvent ->
                run {
                    e.player.inventory.addItemStack(ItemManager.LAZER_RIFLE.item)
                    e.player.inventory.addItemStack(ItemManager.LAZER_SHOTGUN.item)
                }
            }

            eventNode.addListener(EntityDamageEvent::class.java) { e: EntityDamageEvent ->
                run {
                    if (e.entity.entityType != EntityType.PLAYER) return@addListener

                    if (e.entity.health - e.damage <= 0) {
                        e.isCancelled = true
                        e.entity.health = 20f

                        if (e.damageType is EntityDamage) {
                            ((e.damageType as EntityDamage).source as Player).playSound(
                                Sound.sound(
                                    SoundEvent.NOTE_BLOCK_PLING,
                                    Sound.Source.PLAYER,
                                    1f,
                                    1f
                                )
                            )
                        }
                    }

                    val rand = ThreadLocalRandom.current()

                    val entity = Entity(EntityType.ARMOR_STAND)
                    entity.setInstance(
                        e.entity.instance!!,
                        e.entity.eyePosition().subtract(
                            rand.nextDouble(-0.5, 0.5),
                            1.5 + rand.nextDouble(-0.5, 0.5),
                            rand.nextDouble(-0.5, 0.5)
                        )
                    )
                    entity.isCustomNameVisible = true
                    entity.customName = Component.text(e.damage.toInt(), NamedTextColor.RED, TextDecoration.BOLD)
                    entity.isInvisible = true

                    val armourStandMeta = entity.entityMeta as ArmorStandMeta
                    armourStandMeta.setNotifyAboutChanges(false)
                    armourStandMeta.isMarker = true
                    armourStandMeta.isSmall = true
                    armourStandMeta.isHasNoBasePlate = true
                    armourStandMeta.setNotifyAboutChanges(true)

                    object : MinestomRunnable() {
                        var i = 1
                        var accel = 0.5

                        override fun run() {
                            if (i > 10) {
                                entity.remove()
                                cancel()
                                return
                            }

                            entity.position.add(0.0, accel, 0.0)
                            accel *= 0.60

                            i++
                        }
                    }.repeat(3, TimeUnit.TICK).schedule()
                }
            }
        }

    }

}
