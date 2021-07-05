package emortal.lazertag

import emortal.lazertag.game.DeathReason
import emortal.lazertag.game.GameManager
import emortal.lazertag.gun.Gun
import emortal.lazertag.items.ItemManager
import emortal.lazertag.utils.Direction6
import emortal.lazertag.utils.MinestomRunnable
import emortal.lazertag.utils.PlayerUtils.eyePosition
import emortal.lazertag.utils.PlayerUtils.playSound
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.collision.BoundingBox
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.damage.EntityDamage
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.component1
import world.cepi.kstom.util.component2
import world.cepi.kstom.util.component3
import java.time.Duration
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil
import kotlin.math.floor

object EventListener {

    private val mini = MiniMessage.get()

    fun init(extension: Extension) {
        val eventNode = extension.eventNode

        eventNode.listenOnly<ItemDropEvent> {
            isCancelled = true
            if (player.inventory.getItemStack(0).material == ItemManager.KNIFE.material) {

                //Manager.team.createBuilder("team").nameTagVisibility(Name)

            }
            player.inventory.setItemStack(0, ItemManager.KNIFE)
        }

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (hand != Player.Hand.MAIN) return@listenOnly

            val heldGun = Gun.registeredMap[player.inventory.itemInMainHand.meta.customModelData] ?: return@listenOnly
            if (player.itemInMainHand.meta.getTag(Tag.Long("lastShot"))!! > System.currentTimeMillis() - heldGun.cooldown) {
                return@listenOnly
            }
            if (player.itemInMainHand.meta.getTag(Tag.Byte("reloading"))!!.toInt() == 1) {
                return@listenOnly
            }

            player.itemInMainHand = heldGun.itemBuilder.meta { meta: ItemMetaBuilder ->
                meta.set(Tag.Long("lastShot"), System.currentTimeMillis())
            }.build()



            object : MinestomRunnable() {
                var i = heldGun.burstAmount
                val gun = Gun.registeredMap[player.inventory.itemInMainHand.meta.customModelData]!!

                override fun run() {

                    if (player.itemInMainHand.meta.damage > 59) {
                        cancel()
                        player.playSound(Sound.sound(SoundEvent.ITEM_BREAK, Sound.Source.PLAYER, 0.7f, 1.5f))

                        player.sendActionBar(mini.parse("<red>Press <bold><key:key.swapOffhand></bold> to reload!"))
                        return
                    }

                    // If player swaps gun while bursting
                    val newGun = Gun.registeredMap[player.inventory.itemInMainHand.meta.customModelData]
                    if (newGun == null || newGun != gun) {
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

                    player.itemInMainHand = heldGun.itemBuilder.meta { meta: ItemMetaBuilder ->
                        meta.damage(player.itemInMainHand.meta.damage + ceil(59f / heldGun.ammo.toDouble()).toInt())
                    }.build()

                    i--
                }
            }.repeat(Duration.ofMillis(50 * heldGun.burstInterval)).schedule()

        }

        eventNode.listenOnly<EntityTickEvent> {
            if (entity.entityType == EntityType.PLAYER) return@listenOnly

            if (!entity.hasTag(Tag.String("playerUUID"))) return@listenOnly

            entity.velocity = entity.velocity.multiply(1.02) // negates air drag

            //val instance = entity.instance!!
            //instance.sendMovingParticle(Particle.FLAME, entity.position.clone().add(0.0, 0.25, 0.0), entity.velocity, -0.25f)
            // TODO: Move to tick event in gun

            val shooter: Player = Manager.connection.getPlayer(UUID.fromString(entity.getTag(Tag.String("playerUUID"))))!!
            val gun: Int = entity.getTag(Tag.Integer("gunID"))!!
            val boundingBox = entity.boundingBox.expand(1.5, 1.5, 1.5)

            if (entity.aliveTicks > 20*3) {
                Gun.registeredMap[gun]!!.collide(shooter, entity)
            }
            if (intersectsBlock(boundingBox, entity) || entity.isOnGround) {
                Gun.registeredMap[gun]!!.collide(shooter, entity)
            }
            if (intersects(boundingBox, entity.instance!!.getChunkEntities(entity.chunk).filter { it.entityType == EntityType.PLAYER && it != shooter })) {
                Gun.registeredMap[gun]!!.collide(shooter, entity)
            }
        }

        eventNode.listenOnly<PlayerSwapItemEvent> {
            isCancelled = true

            val gun = Gun.registeredMap[offHandItem.meta.customModelData] ?: return@listenOnly
            val game = GameManager.getPlayerGame(player) ?: return@listenOnly

            if (player.itemInMainHand.meta.getTag(Tag.Byte("reloading"))!!.toInt() == 1 || player.itemInMainHand.meta.damage == 0) {
                return@listenOnly
            }

            player.itemInMainHand = gun.itemBuilder.meta { meta: ItemMetaBuilder ->
                meta.damage(60)
                meta.set(Tag.Byte("reloading"), 1)
            }.build()

            game.reloadTasks[player] = object : MinestomRunnable() {
                var i = gun.ammo

                override fun run() {

                    if (i <= 0) {
                        player.playSound(Sound.sound(SoundEvent.IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))
                        Manager.scheduler.buildTask {
                            player.playSound(Sound.sound(SoundEvent.IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))
                        }.delay(Duration.ofMillis(50 * 3L)).schedule()

                        player.itemInMainHand = gun.itemBuilder.meta { meta: ItemMetaBuilder ->
                            meta.damage(0)
                            meta.set(Tag.Byte("reloading"), 0)
                        }.build()

                        cancel()
                        return
                    }

                    player.itemInMainHand = gun.itemBuilder.meta { meta: ItemMetaBuilder ->
                        meta.damage(
                            (player.itemInMainHand.meta.damage - floor(59f / gun.ammo.toDouble()).toInt()).coerceAtMost(
                                59
                            )
                        )
                    }.build()
                    player.playSound(Sound.sound(SoundEvent.ITEM_PICKUP, Sound.Source.PLAYER, 1f, 1f))

                    i--
                }
            }.repeat(Duration.ofMillis(gun.reloadTime / gun.ammo)).schedule()
        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            if (!isFirstSpawn) return@listenOnly

        }

        eventNode.listenOnly<PlayerLoginEvent> {
            val game = GameManager.nextGame()

            setSpawningInstance(game.instance)

            player.scheduleNextTick {
                GameManager.addPlayer(player)
            }
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
            armourStand.customName = Component.text("❤ $damage", NamedTextColor.RED, TextDecoration.BOLD)
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
            }.repeat(Duration.ofMillis(50 * 3L)).schedule()
        }
    }

    private fun intersectsBlock(boundingBox: BoundingBox, entity: Entity): Boolean {
        val (x, y, z) = entity.position

        for (direction in Direction6.values()) {
            if (!entity.instance!!.getBlock((x + direction.x).toInt(), (y + direction.y).toInt(), (z + direction.z).toInt()).isSolid) continue

            if (boundingBox.intersect(x + direction.x.toDouble(), y + direction.y.toDouble(), z + direction.z.toDouble())) {
                return true
            }
        }
        return false
    }

    private fun intersects(boundingBox: BoundingBox, entities: List<Entity>): Boolean = entities.any { boundingBox.intersect(it.boundingBox) }
}

