package emortal.lazertag.game

import emortal.immortal.game.Game
import emortal.immortal.game.GameOptions
import emortal.immortal.game.GameState
import emortal.immortal.game.PvpGame
import emortal.lazertag.gun.Gun
import emortal.lazertag.gun.Gun.Companion.ammoTag
import emortal.lazertag.gun.Gun.Companion.heldGun
import emortal.lazertag.gun.Gun.Companion.lastShotTag
import emortal.lazertag.gun.Gun.Companion.reloadingTag
import emortal.lazertag.maps.MapManager
import emortal.lazertag.utils.setCooldown
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

class LazerTagGame(option: GameOptions) : Game(option), PvpGame {

    companion object {
        val killsTag = Tag.Integer("kills")

    }

    val mini = MiniMessage.get()

    val respawnTasks: ArrayList<Task> = ArrayList()
    val reloadTasks: HashMap<Player, Task> = HashMap()

    override fun playerJoin(player: Player) {
        player.gameMode = GameMode.SPECTATOR
        player.isInvisible = false

        scoreboard?.createLine(Sidebar.ScoreboardLine(
            player.uuid.toString(),

            Component.text()
                .append(Component.text(player.username, NamedTextColor.GRAY))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(0, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .build(),

            0
        ))

        player.setTag(killsTag, 0)

        player.respawnPoint = getRandomRespawnPosition()
        if (player.instance!! != instance) player.setInstance(instance)

    }


    override fun playerLeave(player: Player) {
        player.isInvisible = false

        scoreboard?.removeLine(player.uuid.toString())
    }

    override fun start() {
        startingTask = null
        gameState = GameState.PLAYING

        players.forEach(::respawn)
    }

    override fun startCountdown() {
        start()
    }

    override fun playerDied(player: Player, killer: Entity?) {
        if (gameState == GameState.ENDING) return

        player.inventory.clear()
        player.velocity = Vec(0.0, 0.0, 0.0)
        player.gameMode = GameMode.SPECTATOR
        player.isInvisible = true
        player.sendActionBar(Component.empty())
        player.setNoGravity(true)

        reloadTasks[player]?.cancel()
        reloadTasks.remove(player)

        if (killer != null && killer != player && killer is Player) {
            val lookAtVector = player.position.asVec().sub(killer.position.asVec())

            player.teleport(player.position.withDirection(lookAtVector.mul(-1.0)))
            player.velocity = lookAtVector.normalize().mul(15.0)

            val currentKills = killer.getTag(killsTag)!! + 1

            if (currentKills >= 20) return victory(killer)

            killer.setTag(killsTag, currentKills)

            scoreboard?.updateLineScore(killer.uuid.toString(), currentKills)
            scoreboard?.updateLineContent(
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

            val currentKills = player.getTag(killsTag)!! - 1

            if (currentKills > 0) {
                scoreboard?.updateLineScore(player.uuid.toString(), currentKills)
                scoreboard?.updateLineContent(
                    player.uuid.toString(),
                    Component.text()
                        .append(Component.text(player.username, NamedTextColor.GRAY))
                        .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(currentKills, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                        .build()
                )

                player.setTag(killsTag, currentKills)
            }
        }


        respawnTasks.add(object : MinestomRunnable() {
            var i = 3

            override fun run() {
                if (i == 3) {
                    player.velocity = Vec(0.0, 0.0, 0.0)
                    if (killer != null && !(killer as Player).isDead && killer != player) player.spectate(killer)
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

        instance.entities
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

    override fun registerEvents() = with(eventNode) {
        listenOnly<PlayerChatEvent> {
            if (entity.instance!! != instance) return@listenOnly

            if (player.username == "emortl") {
                setChatFormat {
                    mini.parse("<gradient:light_purple:gold><bold>OWNER</bold></gradient> <gray>emortal: ${it.message}")
                }
            }

        }

        listenOnly<PlayerUseItemEvent> {
            isCancelled = true
            if (entity.instance!! != instance) return@listenOnly
            if (hand != Player.Hand.MAIN) return@listenOnly

            val heldGun = player.heldGun ?: return@listenOnly
            if (player.itemInMainHand.meta.getTag(lastShotTag)!! > System.currentTimeMillis() - heldGun.cooldown) {
                return@listenOnly
            }
            if (player.itemInMainHand.meta.getTag(reloadingTag)!!.toInt() == 1) {
                return@listenOnly
            }

            player.itemInMainHand = player.itemInMainHand.withMeta { meta ->
                meta.set(lastShotTag, System.currentTimeMillis())
            }

            object : MinestomRunnable() {
                var i = heldGun.burstAmount

                override fun run() {

                    if (!player.itemInMainHand.meta.hasTag(ammoTag)) {
                        cancel()
                        return
                    }
                    if (player.itemInMainHand.meta.getTag(ammoTag)!! <= 0) {
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



                    i--
                }
            }.repeat(Duration.of(heldGun.burstInterval, TimeUnit.SERVER_TICK)).schedule()

        }

        listenOnly<EntityTickEvent> {
            if (entity.instance!! != instance) return@listenOnly
            if (entity.entityType == EntityType.PLAYER) return@listenOnly

            if (!entity.hasTag(Gun.playerUUIDTag)) return@listenOnly


            val shooter: Player = Manager.connection.getPlayer(UUID.fromString(entity.getTag(Gun.playerUUIDTag)))!!
            val gun = Gun.registeredMap[entity.getTag(Gun.gunIdTag)!!] ?: return@listenOnly

            // TODO: Better collisions
            if (entity.velocity.x() == 0.0 || entity.velocity.y() == 0.0 || entity.velocity.z() == 0.0) {
                gun.collide(shooter, entity)
            }
            if (entity.aliveTicks > 20*3) {
                gun.collide(shooter, entity)
            }
        }

        listenOnly<PlayerBlockBreakEvent> {
            if (entity.instance!! != instance) return@listenOnly
            isCancelled = true
        }

        listenOnly<PlayerChangeHeldSlotEvent> {
            if (entity.instance!! != instance) return@listenOnly
            isCancelled = true
            player.setHeldItemSlot(0)
        }
        listenOnly<InventoryPreClickEvent> {
            if (entity.instance!! != instance) return@listenOnly
            isCancelled = true
        }
        listenOnly<PlayerBlockBreakEvent> {
            if (entity.instance!! != instance) return@listenOnly
            isCancelled = true
        }
        listenOnly<PlayerBlockPlaceEvent> {
            println("abc")
            if (entity.instance!! != instance) return@listenOnly
            println("cancelled!")
            isCancelled = true
        }
        listenOnly<ItemDropEvent> {
            if (entity.instance!! != instance) return@listenOnly
            isCancelled = true
        }

        listenOnly<PlayerSwapItemEvent> {
            if (entity.instance!! != instance) return@listenOnly
            isCancelled = true

            val gun = Gun.registeredMap[offHandItem.getTag(Gun.gunIdTag)] ?: return@listenOnly

            if (player.itemInMainHand.meta.getTag(reloadingTag)!!.toInt() == 1 || player.itemInMainHand.meta.getTag(ammoTag)!! == gun.ammo) {
                return@listenOnly
            }

            player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                meta.set(ammoTag, 0)
                meta.set(reloadingTag, 1)
            }

            player.setCooldown(player.itemInMainHand.material, gun.reloadTime.toInt() / 50, true)

            reloadTasks[player] = object : MinestomRunnable() {
                var i = gun.ammo

                override fun run() {

                    if (i <= 0) {
                        player.playSound(Sound.sound(SoundEvent.ENTITY_IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))
                        Manager.scheduler.buildTask {
                            player.playSound(Sound.sound(SoundEvent.ENTITY_IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))
                        }.delay(Duration.ofMillis(50 * 3L)).schedule()

                        player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                            meta.set(ammoTag, gun.ammo)
                            meta.set(reloadingTag, 0)
                        }

                        gun.renderAmmo(player, gun.ammo - i)

                        cancel()
                        return
                    }

                    gun.renderAmmo(player, gun.ammo - i)
                    player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                        meta.set(ammoTag, gun.ammo - i)
                    }
                    player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 1f, 1f))

                    i--
                }
            }.repeat(Duration.ofMillis(gun.reloadTime / gun.ammo)).schedule()
        }

        listenOnly<PlayerMoveEvent> {
            if (entity.instance!! != instance) return@listenOnly
            if (player.gameMode != GameMode.ADVENTURE) return@listenOnly

            if (newPosition.y() < 5) {
                kill(player, null)
            }
        }

        listenOnly<EntityDamageEvent> {
            if (entity.instance!! != instance) return@listenOnly
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