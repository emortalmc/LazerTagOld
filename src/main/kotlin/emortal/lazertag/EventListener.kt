package emortal.lazertag

import emortal.lazertag.game.DeathReason
import emortal.lazertag.game.GameManager
import emortal.lazertag.gun.Gun
import emortal.lazertag.utils.MinestomRunnable
import emortal.lazertag.utils.PlayerUtils.eyePosition
import emortal.lazertag.utils.PlayerUtils.playSound
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.damage.EntityDamage
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.event.listenOnly
import java.util.concurrent.ThreadLocalRandom

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

            /*for (i in 0..20) { FLAMMENWERFERR!!!
                val newDir = eyeDir.spread(0.15)

                PlayerUtils.sendParticle(e.player, Particle.FLAME, eyePos.clone().subtract(0.0, 0.25, 0.0), newDir.x.toFloat(), newDir.y.toFloat(), newDir.z.toFloat(), 0, 1f)
            */

            object : MinestomRunnable() {
                var i = heldGun.burstAmount
                val gun = Gun.registeredMap[player.inventory.itemInMainHand.meta.customModelData]!!

                override fun run() {
                    // If player swaps gun while bursting
                    if (Gun.registeredMap[player.inventory.itemInMainHand.meta.customModelData]!! != gun) {
                        cancel()
                        return
                    }

                    if (i < 1) {
                        cancel()
                        return
                    }

                    player.instance!!.playSound(heldGun.sound, player.position)

                    val damageMap = heldGun.shoot(player)

                    damageMap.forEach { (hitEntity, damage) ->
                        player.playSound(Sound.sound(SoundEvent.PLAYER_HURT, Sound.Source.PLAYER, 1f, 1f))

                        player.scheduleNextTick {
                            hitEntity.damage(DamageType.fromPlayer(player), damage)
                        }
                    }

                    i--
                }
            }.repeat(heldGun.burstInterval, TimeUnit.TICK).schedule()



            player.itemInMainHand = heldGun.itemBuilder.meta { meta: ItemMetaBuilder ->
                meta.damage(player.itemInMainHand.meta.damage + (heldGun.maxDurability / heldGun.ammo))
                meta.set(Tag.Long("lastShot"), System.currentTimeMillis())
            }.build()


        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            player.gameMode = GameMode.CREATIVE
        }

        eventNode.listenOnly<PlayerDisconnectEvent> {
            GameManager.getPlayerGame(player)?.removePlayer(player)
        }

        eventNode.listenOnly<PlayerMoveEvent> {
            if (player.gameMode != GameMode.ADVENTURE) return@listenOnly

            if (newPosition.y < 35) {
                GameManager.getPlayerGame(player)?.died(player, null, DeathReason.VOID)
            }
        }

        eventNode.listenOnly<PlayerDeathEvent> {
            player.respawn()
        }

        eventNode.listenOnly<EntityDamageEvent> {
            if (entity.entityType != EntityType.PLAYER) return@listenOnly

            if (damageType !is EntityDamage) {
                isCancelled = true
                return@listenOnly
            }

            val player: Player = entity as Player
            val damager: Player = (damageType as EntityDamage).source as Player

            if (entity.health - damage <= 0) {
                isCancelled = true
                entity.health = 20f

                GameManager.getPlayerGame(player)?.died(player, damager, DeathReason.PLAYER)
            }

            val rand = ThreadLocalRandom.current()

            val armourStand = Entity(EntityType.ARMOR_STAND)

            armourStand.isAutoViewable = false
            armourStand.isInvisible = true
            armourStand.customName = Component.text(damage, NamedTextColor.RED, TextDecoration.BOLD)
            armourStand.isCustomNameVisible = true
            armourStand.setNoGravity(true)

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
}
