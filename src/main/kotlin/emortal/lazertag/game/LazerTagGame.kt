package emortal.lazertag.game

import emortal.immortal.game.Game
import emortal.immortal.game.GameOptions
import emortal.immortal.game.GameState
import emortal.lazertag.gun.Gun
import emortal.lazertag.maps.MapManager
import emortal.lazertag.utils.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.damage.EntityDamage
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.sendMiniMessage
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.*
import java.text.DecimalFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class LazerTagGame(option: GameOptions) : Game(option) {

    // TODO: Maps

    val mini = MiniMessage.get()

    val respawnTasks: ArrayList<Task> = ArrayList()
    val reloadTasks: HashMap<Player, Task> = HashMap()

    override fun playerJoin(player: Player) {
        scoreboard.createLine(Sidebar.ScoreboardLine(
            player.uuid.toString(),

            Component.text()
                .append(Component.text(player.username, NamedTextColor.GRAY))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(0, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .build(),

            0
        ))

        player.setTag(Tag.Integer("kills"), 0)

        player.respawnPoint = getRandomRespawnPosition()
        if (player.instance!! != firstInstance) player.setInstance(firstInstance)

    }

    override fun playerLeave(player: Player) {
        scoreboard.removeLine(player.username)
    }

    override fun start() {
        startingTask = null
        gameState = GameState.PLAYING

        players.forEach(::respawn)
    }

    fun kill(player: Player, killer: Player?) {
        if (gameState == GameState.ENDING) return

        player.inventory.clear()
        player.velocity = Vec(0.0, 0.0, 0.0)
        player.gameMode = GameMode.SPECTATOR
        player.isInvisible = true
        player.setNoGravity(true)

        reloadTasks[player]?.cancel()
        reloadTasks.remove(player)

        if (killer != null && killer != player) {
            val lookAtVector = player.position.asVec().sub(killer.position.asVec())

            player.teleport(player.position.withDirection(lookAtVector.mul(-1.0)))
            player.velocity = lookAtVector.normalize().mul(15.0)

            val currentKills = killer.getTag(Tag.Integer("kills"))!! + 1

            if (currentKills >= 20) return victory(killer)

            killer.setTag(Tag.Integer("kills"), currentKills)

            scoreboard.updateLineScore(killer.uuid.toString(), currentKills)
            scoreboard.updateLineContent(
                killer.uuid.toString(),
                Component.text()
                    .append(Component.text(killer.username, NamedTextColor.GRAY))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(0, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                    .build()
            )

            println("Scoreboard score of ${killer.uuid} should be updated to $currentKills")

            // problematic:  with ${Gun.registeredMap[killer.itemInMainHand.meta.customModelData]!!.name}
            playerAudience.sendMiniMessage(" <red>☠</red> <dark_gray>|</dark_gray> <gray><white>${killer.username}</white> killed <red>${player.username}</red>")
            killer.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.PLAYER, 1f, 1f))

            player.showTitle(Title.title(
                Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                mini.parse("<gray>Killed by <red><bold>${killer.username}</bold></red>"),
                Title.Times.of(
                    Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                )
            ))

            val gun = Gun.registeredMap.values.random()
            killer.inventory.setItemStack(0, gun.item)
            gun.renderAmmo(killer, gun.ammo)
        } else {

            playerAudience.sendMiniMessage(" <red>☠</red> <dark_gray>|</dark_gray> <gray><red>${player.username}</red> killed themselves")

            player.showTitle(Title.title(
                Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                mini.parse("<rainbow>You killed yourself!"),
                Title.Times.of(
                    Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                )
            ))

            val currentKills = player.getTag(Tag.Integer("kills"))!! - 1

            if (currentKills > 0) {
                scoreboard.updateLineScore(player.uuid.toString(), currentKills)
                scoreboard.updateLineContent(
                    player.uuid.toString(),
                    Component.text()
                        .append(Component.text(player.username, NamedTextColor.GRAY))
                        .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(0, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                        .build()
                )

                player.setTag(Tag.Integer("kills"), currentKills)
            }
        }


        respawnTasks.add(object : MinestomRunnable() {
            var i = 3

            override fun run() {
                if (i == 3) {
                    player.velocity = Vec(0.0, 0.0, 0.0)
                    if (killer != null && !killer.isDead && killer != player) player.spectate(killer)
                }
                if (i <= 0) {
                    respawn(player)

                    cancel()
                    return
                }

                val (x, y, z) = killer?.position ?: player.position
                player.playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f), x, y, z)
                player.showTitle(Title.title(
                    Component.text(i, NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.empty(),
                    Title.Times.of(
                        Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                    )
                ))

                i--
            }
        }.delay(Duration.ofSeconds(2)).repeat(Duration.ofSeconds(1)).schedule())
    }
    override fun respawn(player: Player) = with(player) {
        inventory.clear()
        health = 20f
        teleport(getRandomRespawnPosition())
        stopSpectating()
        isInvisible = false
        gameMode = GameMode.ADVENTURE
        setNoGravity(false)
        clearEffects()

        if (gameState == GameState.ENDING) return

        // TODO: Replace with proper gun score system for other modes

        val gun = Gun.registeredMap.values.random()
        inventory.setItemStack(0, gun.item)
        gun.renderAmmo(this, gun.ammo)
    }

    private fun victory(player: Player) {
        if (gameState == GameState.ENDING) return
        gameState = GameState.ENDING

        reloadTasks.values.forEach(Task::cancel)
        reloadTasks.clear()

        firstInstance.entities
            .filter { it !is Player }
            .forEach(Entity::remove)

        players.forEach { it.inventory.clear() }

        val message = Component.text()
            .append(Component.text("VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("\n${player.username} won the game", NamedTextColor.WHITE))
            .build()

        playerAudience.sendMessage(message)

        Manager.scheduler.buildTask { destroy() }
            .delay(12, TimeUnit.SECOND).schedule()
    }

    override fun postDestroy() {
        respawnTasks.forEach(Task::cancel)
        respawnTasks.clear()
    }

    override fun registerEvents() {
        val eventNode = gameTypeInfo.eventNode

        eventNode.listenOnly<PlayerChatEvent> {
            if (player.username == "emortl") {
                setChatFormat {
                    mini.parse("<gradient:light_purple:gold><bold>OWNER</bold></gradient> <gray>emortal: ${it.message}")
                }
            }

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

                    if (!player.itemInMainHand.meta.hasTag(Tag.Integer("ammo"))) {
                        cancel()
                        return
                    }
                    if (player.itemInMainHand.meta.getTag(Tag.Integer("ammo"))!! <= 0) {
                        player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 0.7f, 1.5f))
                        player.sendActionBar(mini.parse("<red>Press <bold><key:key.swapOffhand></bold> to reload!"))

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
                        player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 1f, 1f))

                        player.scheduleNextTick {
                            hitEntity.damage(DamageType.fromPlayer(player), damage)
                        }
                    }

                    val newAmmo = player.itemInMainHand.meta.getTag(Tag.Integer("ammo"))!! - 1
                    heldGun.renderAmmo(player, newAmmo)
                    player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                        meta.set(Tag.Integer("ammo"), newAmmo)
                    }

                    i--
                }
            }.repeat(Duration.of(heldGun.burstInterval, TimeUnit.SERVER_TICK)).schedule()

        }

        eventNode.listenOnly<EntityTickEvent> {
            if (entity.entityType == EntityType.PLAYER) return@listenOnly

            if (!entity.hasTag(Tag.String("playerUUID"))) return@listenOnly

            entity.velocity = entity.velocity.mul(1.6, 1.0, 1.6) // negates air drag

            //val instance = entity.instance!!
            //instance.sendMovingParticle(Particle.FLAME, entity.position.clone().add(0.0, 0.25, 0.0), entity.velocity, -0.25f)
            // TODO: Move to tick event in gun

            val shooter: Player = Manager.connection.getPlayer(UUID.fromString(entity.getTag(Tag.String("playerUUID"))))!!
            val gun: Int = entity.getTag(Tag.Integer("gunID"))!!

            // TODO: Better collisions
            if (entity.velocity.x() == 0.0 || entity.velocity.y() == 0.0 || entity.velocity.z() == 0.0) {
                Gun.registeredMap[gun]!!.collide(shooter, entity)
            }
            if (entity.aliveTicks > 20*3) {
                Gun.registeredMap[gun]!!.collide(shooter, entity)
            }
        }

        eventNode.listenOnly<PlayerBlockBreakEvent> {
            isCancelled = true
        }

        eventNode.listenOnly<PlayerChangeHeldSlotEvent> {
            isCancelled = true
            player.setHeldItemSlot(0)
        }
        eventNode.listenOnly<InventoryPreClickEvent> {
            isCancelled = true
        }
        eventNode.listenOnly<PlayerBlockBreakEvent> {
            isCancelled = true
        }
        eventNode.listenOnly<PlayerBlockPlaceEvent> {
            isCancelled = true
        }
        eventNode.listenOnly<ItemDropEvent> {
            isCancelled = true
        }

        eventNode.listenOnly<PlayerSwapItemEvent> {
            isCancelled = true

            val gun = Gun.registeredMap[offHandItem.meta.customModelData] ?: return@listenOnly

            if (player.itemInMainHand.meta.getTag(Tag.Byte("reloading"))!!.toInt() == 1 || player.itemInMainHand.meta.getTag(Tag.Integer("ammo"))!! == gun.ammo) {
                return@listenOnly
            }

            player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                meta.set(Tag.Integer("ammo"), 0)
                meta.set(Tag.Byte("reloading"), 1)
            }

            reloadTasks[player] = object : MinestomRunnable() {
                var i = gun.ammo

                override fun run() {

                    if (i <= 0) {
                        player.playSound(Sound.sound(SoundEvent.ENTITY_IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))
                        Manager.scheduler.buildTask {
                            player.playSound(Sound.sound(SoundEvent.ENTITY_IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))
                        }.delay(Duration.ofMillis(50 * 3L)).schedule()

                        player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                            meta.set(Tag.Integer("ammo"), gun.ammo)
                            meta.set(Tag.Byte("reloading"), 0)
                        }

                        gun.renderAmmo(player, gun.ammo - i)

                        cancel()
                        return
                    }

                    gun.renderAmmo(player, gun.ammo - i)
                    player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                        meta.set(Tag.Integer("ammo"), gun.ammo - i)
                    }
                    player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 1f, 1f))

                    i--
                }
            }.repeat(Duration.ofMillis(gun.reloadTime / gun.ammo)).schedule()
        }

        eventNode.listenOnly<PlayerMoveEvent> {
            if (player.gameMode != GameMode.ADVENTURE) return@listenOnly

            if (newPosition.y() < 5) {
                kill(player, null)
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

                kill(player, damager)
            }

            val rand = ThreadLocalRandom.current()
            val format = DecimalFormat("0.##")
            val armourStand = Entity(EntityType.ARMOR_STAND)

            armourStand.isAutoViewable = false
            armourStand.isInvisible = true
            armourStand.customName = Component.text("❤ ${format.format(damage)}", NamedTextColor.RED, TextDecoration.BOLD)
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
                entity.eyePosition().sub(
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
                    if (i > 10) {
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

    private fun getRandomRespawnPosition(): Pos = MapManager.spawnPositionMap["dizzymc"]!!.random()
}