package dev.emortal.lazertag.game

import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.game.GameState
import dev.emortal.immortal.game.PvpGame
import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.immortal.util.armify
import dev.emortal.immortal.util.reset
import dev.emortal.lazertag.LazerTagExtension
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
import dev.emortal.lazertag.gun.Rifle
import dev.emortal.lazertag.utils.cancel
import dev.emortal.lazertag.utils.setCooldown
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.metadata.item.ItemEntityMeta
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.ExecutionType
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.event.listenOnly
import java.text.DecimalFormat
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.floor
import kotlin.math.roundToInt

class LazerTagGame(gameOptions: GameOptions) : PvpGame(gameOptions) {

    companion object {
        val destructableBlocks = listOf(Block.GRASS, Block.OAK_LEAVES, Block.SCAFFOLDING, Block.VINE, Block.MOSS_CARPET)
            .map { it.id() }

        private val collidableBlocksBlacklist =
            listOf<Block>(Block.OAK_LEAVES, Block.IRON_BARS, Block.OAK_FENCE).map { it.id() }
        private val collidableBlocksWhitelist = listOf<Block>().map { it.id() }
        val collidableBlocks = Block.values().filter {
            (it.isSolid && !collidableBlocksBlacklist.contains(it.id())) || collidableBlocksWhitelist.contains(it.id())
        }.map { it.id() }
    }

    //TODO: Replace with LazerTag game options?
    private val killsToWin = 25

    val respawnTasks = ConcurrentHashMap<Player, MinestomRunnable>()
    val burstTasks = ConcurrentHashMap<Player, MinestomRunnable>()
    val reloadTasks = ConcurrentHashMap<Player, MinestomRunnable>()

    val bossbarMap = ConcurrentHashMap<Player, Pair<BossBar, Player>>()
    val watchingPlayerMap = ConcurrentHashMap<Player, MutableList<Pair<BossBar, Player>>>()

    var gunRandomizing = true
    var defaultGun: Gun = Rifle
    var destructible = false
    var infiniteAmmo = false

    var eventTask: MinestomRunnable? = null
    var currentEvent: Event? = null

    val damageMap = ConcurrentHashMap<Player, ConcurrentHashMap<Player, Pair<Float, Task>>>()

    override var spawnPosition = Pos(29.5, 15.5, -6.5)

    override fun playerJoin(player: Player) {
        player.gameMode = GameMode.SPECTATOR
        player.isInvisible = false
        player.setCanPickupItem(false)

    }

    override fun playerLeave(player: Player) {
        player.cleanup()
        scoreboard?.removeLine(player.uuid.toString())

        reloadTasks[player]?.cancel()
        burstTasks[player]?.cancel()
        respawnTasks[player]?.cancel()
        reloadTasks.remove(player)
        burstTasks.remove(player)
        respawnTasks.remove(player)
    }

    override fun gameStarted() {
        scoreboard?.removeLine("infoLine")
        gameState = GameState.PLAYING

        players.forEach {
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
        }

        eventTask =
            object : MinestomRunnable(taskGroup = taskGroup, repeat = Duration.ofSeconds(90), delay = Duration.ofSeconds(90)) {
                override fun run() {
                    val randomEvent = Event.createRandomEvent()

                    currentEvent = randomEvent

                    randomEvent.performEvent(this@LazerTagGame)
                }
            }

        players.forEach(::respawn)
    }

    override fun playerDied(player: Player, killer: Entity?) {
        if (gameState == GameState.ENDING) return

        //player.inventory.clear()
        player.velocity = Vec(0.0, 0.0, 0.0)
        player.gameMode = GameMode.SPECTATOR
        player.isInvisible = true
        player.sendActionBar(Component.empty())
        player.setNoGravity(true)

        player.playSound(
            Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.MASTER, 1f, 1f),
            Sound.Emitter.self()
        )

        reloadTasks[player]?.cancel()
        burstTasks[player]?.cancel()
        reloadTasks.remove(player)
        burstTasks.remove(player)

        if (killer != null && killer != player && killer is Player) {
            val kills = ++killer.kills

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

            damageMap[player]?.keys?.forEach {
                if (it == killer) return@forEach

                it.sendMessage(
                    Component.text()
                        .append(Component.text("ASSIST", NamedTextColor.RED))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("You assisted ", NamedTextColor.GRAY))
                        .append(Component.text(killer.username, NamedTextColor.WHITE))
                        .append(Component.text(" in killing ", NamedTextColor.GRAY))
                        .append(Component.text(player.username, NamedTextColor.RED))
                )

                it.playSound(
                    Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 1f, 1f),
                    Sound.Emitter.self()
                )

                it.kills++
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
                entity.setInstance(instance, player.position.add(0.0, 0.5, 0.0))
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
                    "<rainbow>You killed yourself!".asMini(),
                    Title.Times.times(
                        Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                    )
                )
            )
        }

        damageMap.remove(player)

        respawnTasks[player] = object : MinestomRunnable(
            delay = Duration.ofSeconds(2),
            repeat = Duration.ofSeconds(1),
            iterations = 3,
            taskGroup = taskGroup
        ) {
            override fun run() {
                player.playSound(
                    Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f),
                    Sound.Emitter.self()
                )
                player.showTitle(
                    Title.title(
                        Component.text(3 - currentIteration, NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.times(
                            Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(200)
                        )
                    )
                )
            }

            override fun cancelled() {
                respawn(player)
            }
        }
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

        reloadTasks.values.forEach(MinestomRunnable::cancel)
        reloadTasks.clear()

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

        players.forEach {
            it.inventory.clear()
        }
        eventTask?.cancel()
        currentEvent?.eventEnded(this)
    }

    override fun gameDestroyed() {
        reloadTasks.clear()
        burstTasks.clear()
        respawnTasks.clear()

        players.forEach {
            it.cleanup()
        }
    }

    override fun registerEvents() = with(eventNode) {
        listenOnly<PlayerUseItemEvent> {
            isCancelled = true
            if (hand != Player.Hand.MAIN) return@listenOnly

            val heldGun = player.heldGun ?: return@listenOnly
            val lastShotMs = player.itemInMainHand.meta().getTag(lastShotTag) ?: return@listenOnly

            if (heldGun.shootMidReload) {
                val lastAmmo = player.itemInMainHand.meta().getTag(ammoTag)
                if (lastAmmo == 0) return@listenOnly
                reloadTasks[player]?.cancel()
                reloadTasks.remove(player)

                player.setCooldown(player.itemInMainHand.material(), 0, false)

                player.itemInMainHand = player.itemInMainHand.withMeta {
                    it.removeTag(reloadingTag)
                    it.setTag(ammoTag, lastAmmo)
                }
            } else {
                if (player.itemInMainHand.meta().hasTag(reloadingTag)) return@listenOnly
            }


            if (lastShotMs > System.currentTimeMillis() - heldGun.cooldown) {
                return@listenOnly
            }
            if (lastShotMs == 1L) {
                return@listenOnly
            }

            player.itemInMainHand = player.itemInMainHand.withMeta {
                it.setTag(lastShotTag, System.currentTimeMillis())
            }

            val taskSchedule = if (heldGun.burstInterval / 50 <= 0) TaskSchedule.immediate() else TaskSchedule.tick((heldGun.burstInterval / 50).toInt())

            burstTasks[player] = object : MinestomRunnable(
                repeat = taskSchedule,
                iterations = heldGun.burstAmount.toLong(),
                taskGroup = taskGroup
            ) {
                override fun run() {
                    if (!player.itemInMainHand.meta().hasTag(ammoTag)) {
                        cancel()
                        player.sendMessage("cancelled")
                        burstTasks.remove(player)
                        return
                    }

//                    if (player.itemInMainHand.meta().getTag(ammoTag)!! <= 0) {
//                        player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 0.7f, 1.5f))
//                        //player.sendActionBar("<red>Press <bold><key:key.swapOffhand></bold> to reload!".asMini())
//
//
//                        cancel()
//                        burstTasks.remove(player)
//
//                        val gun = Gun.registeredMap[player.itemInMainHand.getTag(Gun.gunIdTag)] ?: return
//                        reload(player, gun)
//                        return
//                    }

                    val damageMap = heldGun.shoot(this@LazerTagGame, player)

                    damageMap.forEach { (hitEntity, damage) ->
                        damage(player, hitEntity, false, damage)
                    }

                    if (player.itemInMainHand.meta().getTag(ammoTag)!! == 0) {
                        // AUTO RELOADLOELDEOKROIJETROIJEOTLJSRGHRLGMlrejkgdklthirlthyi

                        val gun = Gun.registeredMap[player.itemInMainHand.getTag(Gun.gunIdTag)] ?: return
                        reload(player, gun)
                    }
                }
            }

        }

        listenOnly<PlayerTickEvent> {
            val activeRegenEffects = player.activeEffects.firstOrNull { it.potion.effect == PotionEffect.REGENERATION }
            if (activeRegenEffects != null && player.aliveTicks % (50 / (activeRegenEffects.potion.amplifier + 1)) == 0L) {
                player.health += 1f
            }
        }

        cancel<PlayerBlockBreakEvent>()
        cancel<InventoryPreClickEvent>()
        cancel<PlayerBlockBreakEvent>()
        cancel<PlayerBlockPlaceEvent>()
        cancel<ItemDropEvent>()

        listenOnly<PlayerChangeHeldSlotEvent> {
            isCancelled = true
            player.setHeldItemSlot(4)
        }

        listenOnly<PlayerSwapItemEvent> {
            isCancelled = true
            val gun = Gun.registeredMap[this.offHandItem.getTag(Gun.gunIdTag)] ?: return@listenOnly
            reload(player, gun)
        }

        listenOnly<EntityDamageEvent> {
            if (this.entity.health - this.damage <= 0) {
                isCancelled = true
            }
        }

        listenOnly<PlayerMoveEvent> {
            if (player.gameMode != GameMode.ADVENTURE) return@listenOnly

            if (newPosition.y() < 5) {
                val highestDamager = damageMap[player]?.maxByOrNull { it.value.first }?.key

                kill(player, highestDamager)
            }
        }
    }

    fun reload(player: Player, gun: Gun) {
        if (player.itemInMainHand.meta().hasTag(reloadingTag) || player.itemInMainHand.meta().getTag(ammoTag) == gun.ammo) return

        val ammoOnReload = player.itemInMainHand.getTag(ammoTag)!!
        val reloadMillis = gun.getReloadMillis(ammoOnReload)
        player.setCooldown(player.itemInMainHand.material(), (reloadMillis / 50.0).roundToInt(), false)

        val startingAmmo = if (gun.freshReload) 0f else ammoOnReload.toFloat().coerceAtLeast(0f)

        player.itemInMainHand = player.itemInMainHand.withMeta {
            it.setTag(ammoTag, startingAmmo.toInt())
            it.setTag(reloadingTag, 1)
        }

        reloadTasks[player] = object : MinestomRunnable(
            repeat = TaskSchedule.nextTick(),
            iterations = reloadMillis / 50,
            taskGroup = taskGroup
        ) {
            var currentAmmo = startingAmmo

            override fun run() {
                val lastAmmo = currentAmmo
                currentAmmo += (gun.ammo.toFloat() - startingAmmo) / (reloadMillis / 50).toFloat()

                val lastAmmoRounded = floor(lastAmmo).toInt()
                val roundedAmmo = floor(currentAmmo).toInt()

                gun.renderAmmo(player, roundedAmmo, currentAmmo / gun.ammo.toFloat(), reloading = true)
                if (roundedAmmo == lastAmmoRounded) return

                player.itemInMainHand = player.itemInMainHand.withMeta {
                    it.setTag(ammoTag, roundedAmmo)
                }

                player.playSound(
                    Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 0.3f, 2f),
                    Sound.Emitter.self()
                )
            }

            override fun cancelled() {
                player.playSound(Sound.sound(SoundEvent.ENTITY_IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f))
                Manager.scheduler.buildTask {
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
            }
        }
    }

    fun setGun(player: Player, gun: Gun = Gun.randomWithRarity()) {
        burstTasks[player]?.cancel()
        burstTasks.remove(player)
        reloadTasks[player]?.cancel()
        reloadTasks.remove(player)

        player.inventory.setItemStack(4, gun.item)
        gun.renderAmmo(player, gun.ammo)
    }

    @Synchronized fun damage(damager: Player, target: Player, headshot: Boolean = false, damage: Float) {
        if (target.hasSpawnProtection) {
            return
        }

        if (damager.hasSpawnProtection) damager.spawnProtectionMillis = null


        // Assist / Damage credit system
        damageMap.putIfAbsent(target, ConcurrentHashMap())
        damageMap[target]!![damager]?.second?.cancel()

        val removalTask = Manager.scheduler.buildTask {
            damageMap[target]?.remove(damager)
        }.delay(Duration.ofSeconds(6)).schedule()

        damageMap[target]!![damager] = Pair((damageMap[target]!![damager]?.first ?: 0f) + damage, removalTask)

        damager.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 0.75f, 1.1f))

        if (target.health - damage <= 0 && target.gameMode == GameMode.ADVENTURE) {
            target.health = 20f

            watchingPlayerMap[target]?.forEach {
                it.second.hideBossBar(it.first)
                bossbarMap.remove(it.second)
            }
            watchingPlayerMap.remove(target)

            val highestDamager = damageMap[target]?.maxByOrNull { it.value.first }?.key

            kill(target, highestDamager)
            return
        }

        target.scheduleNextTick {
            target.damage(DamageType.fromPlayer(damager), damage)
        }

        if (target == damager) return

        val bossBar = bossbarMap[damager]
        val watchingBossbar = watchingPlayerMap[target]

        watchingBossbar?.forEach {
            it.first.progress((target.health - damage) / target.maxHealth)
        }

        if (bossBar == null || target != bossBar.second) {
            if (bossBar != null) {
                damager.hideBossBar(bossBar.first)
                watchingPlayerMap.remove(bossBar.second)
            }

            val newBossbar = BossBar.bossBar(
                Component.text(target.username, NamedTextColor.GRAY),
                (target.health - damage) / target.maxHealth,
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_10
            )
            var removeTask = Manager.scheduler.buildTask {
                damager.hideBossBar(newBossbar)
                watchingPlayerMap.remove(target)
                bossbarMap.remove(damager)
            }.delay(Duration.ofSeconds(10)).schedule()

            newBossbar.addListener(
                object : BossBar.Listener {
                    override fun bossBarProgressChanged(bar: BossBar, oldProgress: Float, newProgress: Float) {
                        removeTask.cancel()
                        removeTask = Manager.scheduler.buildTask {
                            damager.hideBossBar(newBossbar)
                            watchingPlayerMap.remove(target)
                            bossbarMap.remove(damager)
                        }.delay(Duration.ofSeconds(10)).schedule()
                    }
                }
            )

            bossbarMap[damager] = Pair(newBossbar, target)
            watchingPlayerMap.computeIfAbsent(target) { mutableListOf() }
            watchingPlayerMap[target]?.add(Pair(newBossbar, damager))

            damager.showBossBar(newBossbar)

        }


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
        val task = Manager.scheduler.buildTask {
            healthEntity.velocity = accel
            accel = accel.mul(0.6)
        }
            .repeat(Duration.ofMillis(50))
            .executionType(ExecutionType.SYNC)
            .schedule()

        Manager.scheduler.buildTask {
            task.cancel()
            healthEntity.remove()
        }.delay(Duration.ofMillis(2500)).schedule()

        return
    }

    private fun getRandomRespawnPosition(): Pos {
        return LazerTagExtension.config.spawnPositions["dizzymc"]!!.random()
    }

    override fun instanceCreate(): Instance {
        val lazertagInstance = Manager.instance.createInstanceContainer()
        lazertagInstance.chunkLoader = AnvilLoader("./maps/lazertag/dizzymc/")
        //val file = File("./maps/lazertag/dizzymc.slime")
        //val slimeSource = FileSlimeSource(file)

        //lazertagInstance.chunkLoader = SlimeLoader(lazertagInstance, slimeSource, true)

        return lazertagInstance
        //return Manager.instance.createSharedInstance(LazerTagExtension.lazertagInstance)
    }

}
