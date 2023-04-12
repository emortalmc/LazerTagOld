package dev.emortal.lazertag.game

import dev.emortal.immortal.game.GameState
import dev.emortal.immortal.game.PvpGame
import dev.emortal.immortal.util.armify
import dev.emortal.immortal.util.reset
import dev.emortal.lazertag.LazerTagMain
import dev.emortal.lazertag.event.Event
import dev.emortal.lazertag.game.LazerTagPlayerHelper.cleanup
import dev.emortal.lazertag.game.LazerTagPlayerHelper.hasSpawnProtection
import dev.emortal.lazertag.game.LazerTagPlayerHelper.kills
import dev.emortal.lazertag.game.LazerTagPlayerHelper.spawnProtectionMillis
import dev.emortal.lazertag.gun.Gun
import dev.emortal.lazertag.gun.Gun.Companion.ammoTag
import dev.emortal.lazertag.gun.Gun.Companion.heldGun
import dev.emortal.lazertag.gun.Gun.Companion.lastShotTag
import dev.emortal.lazertag.gun.Gun.Companion.reloadingTag
import dev.emortal.lazertag.gun.ProjectileGun
import dev.emortal.lazertag.gun.Rifle
import dev.emortal.lazertag.raycast.RaycastUtil
import dev.emortal.lazertag.utils.setCooldown
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.metadata.item.ItemEntityMeta
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Supplier
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.floor
import kotlin.math.roundToInt

private val LOGGER = LoggerFactory.getLogger(LazerTagMain::class.java)

class LazerTagGame : PvpGame() {

    companion object {
        val destructableBlocks = listOf(Block.GRASS, Block.OAK_LEAVES, Block.SCAFFOLDING, Block.VINE, Block.MOSS_CARPET)
            .map { it.id() }

        private val collidableBlocksBlacklist =
            listOf<Block>(Block.OAK_LEAVES, Block.IRON_BARS, Block.OAK_FENCE).map { it.id() }
        private val collidableBlocksWhitelist = listOf<Block>().map { it.id() }
        val collidableBlocks = Block.values().filter {
            (it.isSolid && !collidableBlocksBlacklist.contains(it.id())) || collidableBlocksWhitelist.contains(it.id())
        }.map { it.id() }
        
        val miniMessage = MiniMessage.miniMessage()
    }

    override val allowsSpectators = true
    override val countdownSeconds = 30
    override val maxPlayers = 30
    override val minPlayers = 2
    override val showScoreboard = true
    override val canJoinDuringGame = false
    override val showsJoinLeaveMessages = true

    //TODO: Replace with LazerTag game options?
    private val killsToWin = 25

    val burstTasks: MutableMap<UUID, Task> = ConcurrentHashMap<UUID, Task>()
    val reloadTasks: MutableMap<UUID, Task> = ConcurrentHashMap<UUID, Task>()

    var gunRandomizing = true
    var defaultGun: Gun = Rifle
    var infiniteAmmo = false

    var eventTask: Task? = null
    var currentEvent: Event? = null

    var mapName: String? = null

    val damageMap: MutableMap<UUID, MutableMap<UUID, Pair<Float, Task>>> = ConcurrentHashMap<UUID, MutableMap<UUID, Pair<Float, Task>>>()

    var spawnPosition: Pos? = null

    override fun getSpawnPosition(player: Player, spectator: Boolean): Pos {
        return spawnPosition ?: Pos.ZERO
    }

    override fun playerJoin(player: Player) {
        player.gameMode = GameMode.SPECTATOR
        player.isInvisible = false
        player.setCanPickupItem(false)

    }

    override fun playerLeave(player: Player) {
        player.cleanup()
        scoreboard?.removeLine(player.uuid.toString())

        reloadTasks[player.uuid]?.cancel()
        reloadTasks.remove(player.uuid)
        burstTasks[player.uuid]?.cancel()
        burstTasks.remove(player.uuid)
        damageMap.remove(player.uuid)

        damageMap.forEach {
            damageMap[it.key]?.remove(player.uuid)
        }
    }

    override fun gameStarted() {
        // TODO: Remove me
        LOGGER.warn("${RaycastUtil.boundingBoxToArea3dMap.size} map size")

        scoreboard?.removeLine("infoLine")
        gameState = GameState.PLAYING

        players.forEach {
            try {
                scoreboard?.createLine(
                    Sidebar.ScoreboardLine(
                        it.uuid.toString(),

                        Component.text()
                            .append(Component.text(it.username, NamedTextColor.GRAY))
                            .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                            .append(Component.text(0, NamedTextColor.GRAY, TextDecoration.BOLD))
                            .build(),

                        0
                    )
                )
            } catch (e: Exception) {

            }

        }

        instance!!.scheduler().buildTask {
            val randomEvent = Event.createRandomEvent()

            currentEvent = randomEvent

            randomEvent.performEvent(this@LazerTagGame)
        }.repeat(TaskSchedule.seconds(90)).delay(TaskSchedule.seconds(90))

        players.forEach(::respawn)
    }

    override fun playerDied(player: Player, killer: Entity?) {
        if (gameState == GameState.ENDING) return

        //player.inventory.clear()
        player.velocity = Vec.ZERO
        player.gameMode = GameMode.SPECTATOR
        player.isInvisible = true
        player.sendActionBar(Component.empty())
        player.setNoGravity(true)

        player.playSound(
            Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.MASTER, 1f, 1f),
            Sound.Emitter.self()
        )

        reloadTasks[player.uuid]?.cancel()
        burstTasks[player.uuid]?.cancel()
        reloadTasks.remove(player.uuid)
        burstTasks.remove(player.uuid)

        val previousLeader = players.maxBy { it.kills }
        val previousLeadingKills = previousLeader.kills

        if (killer != null && killer != player && killer is Player) {
            val kills = ++killer.kills

            if (kills > previousLeadingKills && previousLeader != killer) {
                sendMessage(
                    Component.text()
                        .append(Component.text("★", NamedTextColor.YELLOW))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(killer.username, NamedTextColor.YELLOW))
                        .append(Component.text(" is now the kill leader!", NamedTextColor.GRAY))
                )
            }

            killer.addEffect(Potion(PotionEffect.REGENERATION, 2, 6 * 20))

            scoreboard?.updateLineScore(killer.uuid.toString(), kills)
            scoreboard?.updateLineContent(
                killer.uuid.toString(),
                Component.text()
                    .append(Component.text(killer.username, NamedTextColor.GRAY))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(
                        Component.text(
                            kills,
                            TextColor.lerp(
                                kills.toFloat() / killsToWin.toFloat(),
                                NamedTextColor.GRAY,
                                NamedTextColor.LIGHT_PURPLE
                            ),
                            TextDecoration.BOLD
                        )
                    )
                    .build()
            )

            val gunName = killer.heldGun?.name ?: "nothing apparently"

            if (gunName == "Trumpet") {
                player.playSound(
                    Sound.sound(Key.key("entity.roblox.death"), Sound.Source.MASTER, 1f, 1f),
                    Sound.Emitter.self()
                )
                killer.playSound(
                    Sound.sound(Key.key("entity.roblox.death"), Sound.Source.MASTER, 1f, 1f),
                    Sound.Emitter.self()
                )
            } else {
                killer.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.PLAYER, 1f, 1f))
            }

            sendMessage(
                Component.text()
                    .append(Component.text("☠", NamedTextColor.RED))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(killer.username, NamedTextColor.WHITE))
                    .append(Component.text(" killed ", NamedTextColor.GRAY))
                    .append(Component.text(player.username, NamedTextColor.RED))
                    .append(Component.text(" with ", NamedTextColor.GRAY))
                    .append(Component.text(gunName, NamedTextColor.GOLD))
            )

            killer.showTitle(
                Title.title(
                    Component.empty(),
                    Component.text()
                        .append(Component.text("☠ ", NamedTextColor.RED))
                        .append(Component.text(player.username, NamedTextColor.RED))
                        .build(),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500))
                )
            )

            damageMap[player.uuid]?.keys?.forEach { uuid ->
                if (uuid == killer.uuid) return@forEach

                val player = players.firstOrNull { it.uuid == uuid } ?: return@forEach

                player.sendMessage(
                    Component.text()
                        .append(Component.text("ASSIST", NamedTextColor.RED))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("You assisted ", NamedTextColor.GRAY))
                        .append(Component.text(killer.username, NamedTextColor.WHITE))
                        .append(Component.text(" in killing ", NamedTextColor.GRAY))
                        .append(Component.text(player.username, NamedTextColor.RED))
                )

                player.playSound(
                    Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 1f, 1f),
                    Sound.Emitter.self()
                )

                player.kills++
            }

            player.showTitle(
                Title.title(
                    Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                    Component.text()
                        .append(Component.text("Killed by ", NamedTextColor.GRAY))
                        .append(Component.text(killer.username, NamedTextColor.RED, TextDecoration.BOLD))
                        .build(),
                    Title.Times.times(
                        Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                    )
                )
            )

            val coinItem = ItemStack.of(Material.REDSTONE)
            val random = ThreadLocalRandom.current()
            repeat(10) {
                val entity = Entity(EntityType.ITEM)
                val meta = entity.entityMeta as ItemEntityMeta
                meta.item = coinItem

                entity.velocity = Vec(
                    random.nextDouble(-1.0, 1.0) * 5,
                    random.nextDouble(0.5, 1.0) * 10,
                    random.nextDouble(-1.0, 1.0) * 5
                )

                entity.isAutoViewable = false
                entity.scheduleRemove(Duration.ofSeconds(3))
                entity.setInstance(instance!!, player.position.add(0.0, 0.5, 0.0))
                entity.addViewer(killer)
            }

            if (kills >= killsToWin) return victory(killer)

            if (gunRandomizing) setGun(killer)
        } else {
            sendMessage(
                Component.text()
                    .append(Component.text("☠", NamedTextColor.RED))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(player.username, NamedTextColor.RED))
                    .append(Component.text(" killed themselves", NamedTextColor.GRAY))
            )

            player.showTitle(
                Title.title(
                    Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                    miniMessage.deserialize("<rainbow>You killed yourself!"),
                    Title.Times.times(
                        Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                    )
                )
            )
        }

        damageMap.remove(player.uuid)

        damageMap.forEach {
            damageMap[it.key]?.remove(player.uuid)
        }

        // Respawn task
        player.scheduler().submitTask(object : Supplier<TaskSchedule> {
            var secondsLeft = 4
            
            override fun get(): TaskSchedule {
                if (secondsLeft == 4) {
                    secondsLeft--
                    return TaskSchedule.seconds(2) // first iter, delay for 2 secs
                }
                
                if (secondsLeft == 0) {
                    respawn(player)
                    return TaskSchedule.stop()
                }

                player.playSound(
                    Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f),
                    Sound.Emitter.self()
                )
                player.showTitle(
                    Title.title(
                        Component.text(secondsLeft, NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.times(
                            Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(200)
                        )
                    )
                )

                secondsLeft--;
                
                return TaskSchedule.seconds(1)
            }
        })
    }

    override fun respawn(player: Player) = with(player) {
        spawnProtectionMillis = 3500
        reset()
        teleport(getRandomRespawnPosition())
        setHeldItemSlot(4)

        player.playSound(
            Sound.sound(SoundEvent.BLOCK_BEACON_ACTIVATE, Sound.Source.MASTER, 1f, 2f),
            Sound.Emitter.self()
        )

        if (gameState == GameState.ENDING) return

        if (gunRandomizing) setGun(player)
        else setGun(player, defaultGun)
    }

    override fun gameWon(winningPlayers: Collection<Player>) {
        val winningPlayer = winningPlayers.first()

        val message = Component.text()
            .append(Component.text(" ${" ".repeat(25)}VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("\n\n Winner: ", NamedTextColor.GRAY))
            .append(Component.text(winningPlayer.username, NamedTextColor.GREEN))

        message.append(Component.newline())

        players.sortedBy { it.kills }.reversed().take(3).forEach { plr ->
            message.append(
                Component.text()
                    .append(Component.newline())
                    .append(Component.space())
                    .append(Component.text(plr.username, NamedTextColor.GRAY))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(plr.kills, NamedTextColor.WHITE))
            )
        }

        sendMessage(message.armify())

        eventTask?.cancel()
        currentEvent?.eventEnded(this)

        players.forEach {
            it.inventory.clear()
        }
    }

    override fun gameEnded() {
        reloadTasks.values.forEach {
            it.cancel()
        }
        reloadTasks.clear()
        burstTasks.values.forEach {
            it.cancel()
        }
        burstTasks.clear()
        damageMap.clear()

        eventTask?.cancel()
        eventTask = null

        players.forEach {
            it.cleanup()
        }
    }

    override fun registerEvents(eventNode: EventNode<InstanceEvent>) {
        eventNode.addListener(ProjectileCollideWithBlockEvent::class.java) { e ->
            val gunId = e.entity.getTag(Gun.gunIdTag) ?: return@addListener
            val shooterUUID = e.entity.getTag(Gun.shooterTag) ?: return@addListener
            val gunClass = Gun.registeredMap[gunId] as? ProjectileGun ?: return@addListener

            val shooter = players.firstOrNull { it.uuid == shooterUUID } ?: return@addListener
            gunClass.collided(this, shooter, e.entity)
            e.entity.remove()
        }

        eventNode.addListener(ProjectileCollideWithEntityEvent::class.java) { e ->
            val gunId = e.entity.getTag(Gun.gunIdTag) ?: return@addListener
            val shooterUUID = e.entity.getTag(Gun.shooterTag) ?: return@addListener
            val gunClass = Gun.registeredMap[gunId] as? ProjectileGun ?: return@addListener

            val playerTarget = e.target as? Player ?: return@addListener
            val shooter = players.firstOrNull { it.uuid == shooterUUID } ?: return@addListener
            if (shooter == playerTarget) return@addListener

            gunClass.collidedWithEntity(this, shooter, e.entity, listOf(playerTarget))
            e.entity.remove()
        }

        eventNode.addListener(PlayerUseItemEvent::class.java) { e ->
            val player = e.player
            
            e.isCancelled = true
            if (e.hand != Player.Hand.MAIN) return@addListener

            val heldGun = player.heldGun ?: return@addListener
            val lastShotMs = player.itemInMainHand.meta().getTag(lastShotTag) ?: return@addListener

            if (heldGun.shootMidReload) {
                val lastAmmo = player.itemInMainHand.meta().getTag(ammoTag)
                if (lastAmmo == 0) return@addListener
                reloadTasks[player.uuid]?.cancel()
                reloadTasks.remove(player.uuid)

                player.setCooldown(player.itemInMainHand.material(), 0, false)

                player.itemInMainHand = player.itemInMainHand.withMeta {
                    it.removeTag(reloadingTag)
                    it.setTag(ammoTag, lastAmmo)
                }
            } else {
                if (player.itemInMainHand.meta().hasTag(reloadingTag)) return@addListener
            }


            if (lastShotMs > System.currentTimeMillis() - heldGun.cooldown) {
                return@addListener
            }
            if (lastShotMs == 1L) {
                return@addListener
            }

            player.itemInMainHand = player.itemInMainHand.withMeta {
                it.setTag(lastShotTag, System.currentTimeMillis())
            }

            val taskSchedule = if (heldGun.burstInterval / 50 <= 0) TaskSchedule.immediate() else TaskSchedule.tick(heldGun.burstInterval / 50)

            burstTasks[player.uuid] = player.scheduler().submitTask(object : Supplier<TaskSchedule> {
                var burst = heldGun.burstAmount

                override fun get(): TaskSchedule {
                    if (burst == 0) {
                        return TaskSchedule.stop()
                    }

                    if (!player.itemInMainHand.meta().hasTag(ammoTag)) {
                        return TaskSchedule.stop()
                    }

                    val damageMap = heldGun.shoot(this@LazerTagGame, player)

                    damageMap.forEach { (hitEntity, damage) ->
                        damage(player, hitEntity, false, damage)
                    }

                    if (player.itemInMainHand.meta().getTag(ammoTag) == 0) {
                        // AUTO RELOADLOELDEOKROIJETROIJEOTLJSRGHRLGMlrejkgdklthirlthyi

                        val gun = Gun.registeredMap[player.itemInMainHand.getTag(Gun.gunIdTag)] ?: return taskSchedule
                        reload(player, gun)
                    }

                    burst--;

                    return taskSchedule
                }
            })

        }

        eventNode.addListener(PlayerTickEvent::class.java) { e ->
            val activeRegenEffects = e.player.activeEffects.firstOrNull { it.potion.effect == PotionEffect.REGENERATION }
            if (activeRegenEffects != null && e.player.aliveTicks % (50 / (activeRegenEffects.potion.amplifier + 1)) == 0L) {
                e.player.health += 1f
            }
        }

        eventNode.addListener(PlayerBlockBreakEvent::class.java) { e ->
            e.isCancelled = true
        }
        eventNode.addListener(InventoryPreClickEvent::class.java) { e ->
            e.isCancelled = true
        }
        eventNode.addListener(PlayerBlockPlaceEvent::class.java) { e ->
            e.isCancelled = true
        }
        eventNode.addListener(ItemDropEvent::class.java) { e ->
            e.isCancelled = true
        }

        eventNode.addListener(PlayerChangeHeldSlotEvent::class.java) { e ->
            e.isCancelled = true
            e.player.setHeldItemSlot(4)
        }

        eventNode.addListener(PlayerSwapItemEvent::class.java) { e ->
            e.isCancelled = true
            val gun = Gun.registeredMap[e.offHandItem.getTag(Gun.gunIdTag)] ?: return@addListener
            reload(e.player, gun)
        }

        eventNode.addListener(EntityDamageEvent::class.java) { e ->
            if (e.entity.health - e.damage <= 0) {
                e.isCancelled = true
            }
        }

        eventNode.addListener(PlayerMoveEvent::class.java) { e ->
            if (e.player.gameMode != GameMode.ADVENTURE) return@addListener

            if (e.newPosition.y < -7) {
                val highestDamager = damageMap[e.player.uuid]?.maxByOrNull { it.value.first }?.key
                val killer = players.firstOrNull { it.uuid == highestDamager }

                kill(e.player, killer)
            }
        }
    }

    fun reload(player: Player, gun: Gun) {
        if (player.itemInMainHand.meta().hasTag(reloadingTag) || player.itemInMainHand.meta().getTag(ammoTag) == gun.ammo) return

        val ammoOnReload = player.itemInMainHand.getTag(ammoTag) ?: return
        val reloadMillis = gun.getReloadMillis(ammoOnReload)
        player.setCooldown(player.itemInMainHand.material(), (reloadMillis / 50.0).roundToInt(), false)

        val startingAmmo = if (gun.freshReload) 0f else ammoOnReload.toFloat().coerceAtLeast(0f)

        player.itemInMainHand = player.itemInMainHand.withMeta {
            it.setTag(ammoTag, startingAmmo.toInt())
            it.setTag(reloadingTag, 1)
        }

        reloadTasks[player.uuid] = player.scheduler().submitTask(object : Supplier<TaskSchedule> {
            var reloadIter = reloadMillis / 50
            var currentAmmo = startingAmmo

            override fun get(): TaskSchedule {
                if (reloadIter == 0) {
                    player.playSound(Sound.sound(SoundEvent.ENTITY_IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))
                    player.scheduler().buildTask {
                        player.playSound(
                            Sound.sound(
                                SoundEvent.ENTITY_IRON_GOLEM_ATTACK,
                                Sound.Source.PLAYER,
                                1f,
                                1f
                            ),
                            Sound.Emitter.self()
                        )
                    }.delay(Duration.ofMillis(50 * 3L)).schedule()

                    player.itemInMainHand = player.itemInMainHand.withMeta {
                        it.setTag(ammoTag, gun.ammo)
                        it.removeTag(reloadingTag)
                    }

                    gun.renderAmmo(player, gun.ammo)

                    return TaskSchedule.stop()
                }

                reloadIter--

                val lastAmmo = currentAmmo
                currentAmmo += (gun.ammo.toFloat() - startingAmmo) / (reloadMillis / 50).toFloat()

                val lastAmmoRounded = floor(lastAmmo).toInt()
                val roundedAmmo = floor(currentAmmo).toInt()

                gun.renderAmmo(player, roundedAmmo, currentAmmo / gun.ammo.toFloat(), reloading = true)
                if (roundedAmmo == lastAmmoRounded) return TaskSchedule.nextTick()

                player.itemInMainHand = player.itemInMainHand.withMeta {
                    it.setTag(ammoTag, roundedAmmo)
                }

                player.playSound(
                    Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 0.3f, 2f),
                    Sound.Emitter.self()
                )

                return TaskSchedule.nextTick()
            }
        })
    }

    fun setGun(player: Player, gun: Gun = Gun.randomWithRarity()) {
        burstTasks[player.uuid]?.cancel()
        burstTasks.remove(player.uuid)
        reloadTasks[player.uuid]?.cancel()
        reloadTasks.remove(player.uuid)

        player.inventory.setItemStack(4, gun.item)
        gun.renderAmmo(player, gun.ammo)
    }

    fun damage(damager: Player, target: Player, headshot: Boolean = false, damage: Float) {
        if (target.hasSpawnProtection) return
        if (damager.hasSpawnProtection) damager.spawnProtectionMillis = null


        // Assist / Damage credit system
        // This is horrible.
        if (damager != target) {
            damageMap.putIfAbsent(target.uuid, ConcurrentHashMap())
            damageMap[target.uuid]?.get(damager.uuid)?.second?.cancel()

            val removalTask = target.scheduler().buildTask {
                damageMap[target.uuid]?.remove(damager.uuid)
            }.delay(Duration.ofSeconds(6)).schedule()

            damageMap[target.uuid]?.set(damager.uuid, Pair((damageMap[target.uuid]?.get(damager.uuid)?.first ?: 0f) + damage, removalTask))
        }


        damager.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 0.75f, 1.1f))

        if (target.health - damage <= 0 && target.gameMode == GameMode.ADVENTURE) {
            target.health = 20f

            val highestDamager = damageMap[target.uuid]?.maxByOrNull { it.value.first }?.key
            val player = players.firstOrNull { it.uuid == highestDamager } ?: return

            kill(target, player)
            return
        }

        target.scheduleNextTick {
            target.damage(DamageType.fromPlayer(damager), damage)
        }

        if (target == damager) return

        val rand = ThreadLocalRandom.current()
        val format = DecimalFormat("0.##")

        val healthEntity = Entity(EntityType.ARMOR_STAND)
        val healthEntityMeta = healthEntity.entityMeta as ArmorStandMeta

        healthEntity.isAutoViewable = false
        healthEntity.isInvisible = true
        healthEntity.customName =
            Component.text()
                .append(Component.text("❤ "))
                .append(Component.text(format.format(damage), NamedTextColor.WHITE, TextDecoration.BOLD))
                .build()
                .color(TextColor.lerp(damage / 20f, TextColor.color(255, 150, 150), NamedTextColor.DARK_RED))
        healthEntity.isCustomNameVisible = true
        healthEntity.setNoGravity(true)
        healthEntityMeta.setNotifyAboutChanges(false)
        healthEntityMeta.isMarker = true
        healthEntityMeta.isSmall = true
        healthEntityMeta.isHasNoBasePlate = true
        healthEntityMeta.setNotifyAboutChanges(true)

        healthEntity.setInstance(
            target.instance!!,
            target.position.add(0.0, 1.0, 0.0)
            //.sub(
            //    rand.nextDouble(-0.5, 0.5),
            //    rand.nextDouble(0.5) - 0.5,
            //    rand.nextDouble(-0.5, 0.5)
            //)
        )

        healthEntity.addViewer(damager)

        var accel = Vec(rand.nextDouble(-5.0, 5.0), rand.nextDouble(2.5, 5.0), rand.nextDouble(-5.0, 5.0))
        val task = healthEntity.scheduler().buildTask {
            healthEntity.velocity = accel
            accel = accel.mul(0.6)
        }
            .repeat(Duration.ofMillis(50))
            .schedule()

        healthEntity.scheduler().buildTask {
            task.cancel()
            healthEntity.remove()
        }.delay(Duration.ofMillis(2500)).schedule()

        return
    }

    private fun getRandomRespawnPosition(): Pos {
        return LazerTagMain.config.spawnPositions[mapName]!!.random()
    }

    override fun instanceCreate(): CompletableFuture<Instance> {
        val randomMap = LazerTagMain.maps.random()

        mapName = randomMap

        val instanceFuture = CompletableFuture<Instance>()
        val lazertagInstance = MinecraftServer.getInstanceManager().createInstanceContainer()

        if (mapName == "arena") {
            lazertagInstance.time = 13000
            spawnPosition = Pos(0.5, 70.0, 0.5)
        } else {
            spawnPosition = Pos(29.5, 15.5, -6.5)
        }

        lazertagInstance.timeRate = 0
        lazertagInstance.timeUpdate = null

        lazertagInstance.chunkLoader = AnvilLoader("./maps/lazertag/$randomMap")
        lazertagInstance.setTag(Tag.Boolean("doNotAutoUnloadChunk"), true)
        lazertagInstance.enableAutoChunkLoad(false)

        val radius = 5
        val chunkFutures = mutableListOf<CompletableFuture<Chunk>>()
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                chunkFutures.add(lazertagInstance.loadChunk(x, z))
            }
        }

        CompletableFuture.allOf(*chunkFutures.toTypedArray()).thenRun {
            instanceFuture.complete(lazertagInstance)
        }

        return instanceFuture
    }

}
