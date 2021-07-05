package emortal.lazertag

import emortal.lazertag.game.DeathReason
import emortal.lazertag.game.GameManager
import emortal.lazertag.gun.Gun
import emortal.lazertag.utils.MinestomRunnable
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
import net.minestom.server.utils.Direction
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.*
import java.time.Duration
import java.util.*
import java.util.concurrent.ThreadLocalRandom

object EventListener {

    private val mini = MiniMessage.get()

    fun init(extension: Extension) {
        val eventNode = extension.eventNode

        eventNode.listenOnly<ItemDropEvent> {
            isCancelled = true
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

            player.itemInMainHand = player.itemInMainHand.withMeta { meta ->
                meta.set(Tag.Long("lastShot"), System.currentTimeMillis())
            }


            object : MinestomRunnable() {
                var i = heldGun.burstAmount

                override fun run() {

                    if (player.itemInMainHand.meta.damage > 59) {
                        cancel()
                        player.playSound(Sound.sound(SoundEvent.ITEM_BREAK, Sound.Source.PLAYER, 0.7f, 1.5f))

                        player.sendActionBar(mini.parse("<red>Press <bold><key:key.swapOffhand></bold> to reload!"))
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

                    player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                        meta.set(Tag.Integer("ammo"), (heldGun.ammo - 1).also {
                            heldGun.renderAmmo(player, it)
                        })
                    }

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
            // Double bounding box in size
            val boundingBox = entity.boundingBox.expand(entity.boundingBox.width, entity.boundingBox.height, entity.boundingBox.depth)

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

        eventNode.listenOnly<PlayerChangeHeldSlotEvent> {
            isCancelled = true
            player.setHeldItemSlot(0)
        }

        eventNode.listenOnly<PlayerSwapItemEvent> {
            isCancelled = true

            val gun = Gun.registeredMap[offHandItem.meta.customModelData] ?: return@listenOnly
            val game = GameManager[player] ?: return@listenOnly

            if (player.itemInMainHand.meta.getTag(Tag.Byte("reloading"))!!.toInt() == 1 || player.itemInMainHand.meta.getTag(Tag.Integer("ammo"))!! == gun.ammo) {
                return@listenOnly
            }

            player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                meta.set(Tag.Integer("ammo"), 0)
                meta.set(Tag.Byte("reloading"), 1)
            }

            game.reloadTasks[player] = object : MinestomRunnable() {
                var i = gun.ammo

                override fun run() {

                    if (i <= 0) {
                        player.playSound(Sound.sound(SoundEvent.IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))
                        Manager.scheduler.buildTask {
                            player.playSound(Sound.sound(SoundEvent.IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))
                        }.delay(Duration.ofMillis(50 * 3L)).schedule()

                        player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                            meta.set(Tag.Integer("ammo"), gun.ammo)
                            meta.set(Tag.Byte("reloading"), 0)
                        }

                        cancel()
                        return
                    }

                    gun.renderAmmo(player, gun.ammo - i)
                    player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                        meta.set(Tag.Integer("ammo"), gun.ammo - i)
                    }
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
            GameManager[player]?.removePlayer(player)
        }

        eventNode.listenOnly<PlayerMoveEvent> {
            if (player.gameMode != GameMode.ADVENTURE) return@listenOnly

            if (newPosition.y < 35) {
                GameManager[player]?.kill(player, null, DeathReason.VOID)
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

                GameManager[player]?.kill(player, damager, DeathReason.PLAYER)
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

        for (direction in Direction.values()) {
            if (!entity.instance!!.getBlock((x + direction.normalX()).toInt(), (y + direction.normalY()).toInt(), (z + direction.normalZ()).toInt()).isSolid) continue

            if (boundingBox.intersect(x + direction.normalX().toDouble(), y + direction.normalY().toDouble(), z + direction.normalZ().toDouble())) {
                return true
            }
        }
        return false
    }

    private fun intersects(boundingBox: BoundingBox, entities: List<Entity>): Boolean = entities.any { boundingBox.intersect(it.boundingBox) }
}

