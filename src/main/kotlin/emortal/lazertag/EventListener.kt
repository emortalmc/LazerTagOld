package emortal.lazertag

import emortal.lazertag.game.GameManager
import emortal.lazertag.gun.Gun
import emortal.lazertag.items.ItemManager
import emortal.lazertag.utils.MinestomRunnable
import emortal.lazertag.utils.ParticleUtils
import emortal.lazertag.utils.PlayerUtils.eyePosition
import emortal.lazertag.utils.RandomUtils
import emortal.lazertag.utils.RandomUtils.spread
import io.github.bloepiloepi.particles.shapes.ParticleShape
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.color.Color
import net.minestom.server.entity.*
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.damage.EntityDamage
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.utils.Position
import net.minestom.server.utils.Vector
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.raycast.HitType
import world.cepi.kstom.raycast.RayCast
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

object EventListener {

    fun init(extension: Extension) {
        val eventNode = extension.eventNode

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (hand != Player.Hand.MAIN) return@listenOnly

            val instance = player.instance!!
            val threadLocalRandom = ThreadLocalRandom.current()

            val heldGun = Gun.registeredMap[player.inventory.itemInMainHand.meta.customModelData]!!
            if (player.itemInMainHand.meta.getTag(Tag.Long("lastShot"))!! > System.currentTimeMillis() - heldGun.cooldown) {
                return@listenOnly
            }

            /*for (i in 0..20) {
                val newDir = eyeDir.spread(0.15)

                PlayerUtils.sendParticle(e.player, Particle.FLAME, eyePos.clone().subtract(0.0, 0.25, 0.0), newDir.x.toFloat(), newDir.y.toFloat(), newDir.z.toFloat(), 0, 1f)
            }*/

            player.itemInMainHand = heldGun.itemBuilder.meta { meta: ItemMetaBuilder ->
                meta.damage(player.itemInMainHand.meta.damage + (heldGun.maxDurability / heldGun.ammo))
                meta.set(Tag.Long("lastShot"), System.currentTimeMillis())
            }.build()

            if (heldGun.burstAmount != 0) {
                object : MinestomRunnable() {
                    var i = heldGun.burstAmount

                    override fun run() {

                        // If player swaps gun while bursting
                        if (Gun.registeredMap[player.inventory.itemInMainHand.meta.customModelData]!! != heldGun) {
                            cancel()
                            return
                        }

                        if (i < 1) {
                            cancel()
                            return
                        }

                        val eyePos = player.eyePosition()
                        val eyeDir = eyePos.direction

                        shoot(instance, player, heldGun, eyeDir, eyePos)

                        i--
                    }
                }.repeat(heldGun.burstInterval, TimeUnit.TICK).schedule()

                return@listenOnly
            }

            val eyePos = player.eyePosition()
            val eyeDir = eyePos.direction
            shoot(instance, player, heldGun, eyeDir, eyePos)

        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            player.inventory.addItemStack(ItemManager.LAZER_MINIGUN.item)
            player.inventory.addItemStack(ItemManager.LAZER_SHOTGUN.item)
            player.inventory.addItemStack(ItemManager.RIFLE.item)
            player.inventory.addItemStack(ItemManager.RAILGUN.item)
            player.gameMode = GameMode.CREATIVE
        }

        eventNode.listenOnly<PlayerMoveEvent> {
            if (newPosition.y < 35) {
                GameManager.getPlayerGame(player)?.died(player)
            }
        }

        eventNode.listenOnly<EntityDamageEvent> {
            if (entity.entityType != EntityType.PLAYER) return@listenOnly

            val damager: Player = (damageType as EntityDamage).source as Player

            if (entity.health - damage <= 0) {
                isCancelled = true
                entity.health = 20f

                damager.playSound(
                    Sound.sound(
                        SoundEvent.NOTE_BLOCK_PLING,
                        Sound.Source.PLAYER,
                        1f,
                        1f
                    )
                )
            }

            val rand = ThreadLocalRandom.current()

            val armourStand = Entity(EntityType.ARMOR_STAND)

            armourStand.isAutoViewable = false
            armourStand.isInvisible = true
            armourStand.customName = Component.text(damage, NamedTextColor.RED, TextDecoration.BOLD)
            armourStand.isCustomNameVisible = true

            val armourStandMeta = armourStand.entityMeta as ArmorStandMeta
            armourStandMeta.setNotifyAboutChanges(false)
            armourStandMeta.isMarker = true
            armourStandMeta.isSmall = true
            armourStandMeta.isHasNoBasePlate = true
            armourStandMeta.setNotifyAboutChanges(true)

            armourStand.setInstance(
                entity.instance!!,
                entity.eyePosition().subtract(
                    rand.nextDouble(-0.5, 0.5),
                    rand.nextDouble(0.5),
                    rand.nextDouble(-0.5, 0.5)
                )
            )

            armourStand.addViewer(damager)

            object : MinestomRunnable() {
                var i = 1
                var accel = 0.5

                override fun run() {
                    if (i > 7) {
                        armourStand.remove()
                        cancel()
                        return
                    }


                    armourStand.position.add(0.0, accel, 0.0)
                    accel *= 0.60

                    i++
                }
            }.repeat(3, TimeUnit.TICK).schedule()
        }
    }

    private fun shoot(instance: Instance, player: Player, heldGun: Gun, eyeDir: Vector, eyePos: Position) {
        val damageMap = HashMap<LivingEntity, Float>()

        heldGun.shoot(player)

        repeat(heldGun.numberOfBullets) {

            val direction = eyeDir.spread(heldGun.spread).normalize()

            val raycast = RayCast.castRay(
                instance,
                player,
                eyePos.toVector(),
                direction,
                heldGun.maxDistance,
                0.5,
                margin = 0.3
            )
            val lastPos = raycast.finalPosition.toPosition()

            if (raycast.hitType == HitType.ENTITY) {
                val hitEntity = raycast.hitEntity!!

                val shapeOptions = ParticleUtils.getColouredShapeOptions(Color(255, 0, 0), Color(20, 20, 20), 1.5f)
                ParticleShape.line(raycast.finalPosition.subtract(direction.multiply(6)).toPosition(), lastPos)
                    .iterator(shapeOptions).draw(instance, RandomUtils.ZERO_POS)

                damageMap[hitEntity] = damageMap.getOrDefault(hitEntity, 0f) + heldGun.damage
            } else {
                val shapeOptions = ParticleUtils.getColouredShapeOptions(Color(100, 100, 100), Color(50, 50, 50), 0.2f)
                ParticleShape.line(eyePos, lastPos)
                    .iterator(shapeOptions).draw(instance, RandomUtils.ZERO_POS)
            }

        }

        damageMap.forEach { (hitEntity, damage) ->
            player.playSound(Sound.sound(SoundEvent.PLAYER_HURT, Sound.Source.PLAYER, 1f, 1f))

            hitEntity.damage(DamageType.fromPlayer(player), damage)
        }
    }



}
