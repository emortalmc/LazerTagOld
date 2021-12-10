package emortal.lazertag.game

import dev.emortal.immortal.game.EndGameQuotes
import dev.emortal.immortal.game.GameOptions
import dev.emortal.immortal.game.GameState
import dev.emortal.immortal.game.PvpGame
import dev.emortal.immortal.util.reset
import emortal.lazertag.LazerTagExtension
import emortal.lazertag.gun.Gun
import emortal.lazertag.gun.Gun.Companion.ammoTag
import emortal.lazertag.gun.Gun.Companion.heldGun
import emortal.lazertag.gun.Gun.Companion.lastShotTag
import emortal.lazertag.gun.Gun.Companion.reloadingTag
import emortal.lazertag.gun.ProjectileGun
import emortal.lazertag.utils.cancel
import emortal.lazertag.utils.setCooldown
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
import net.minestom.server.entity.damage.EntityDamage
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.message.Messenger.sendMessage
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.item.and
import world.cepi.kstom.util.MinestomRunnable
import world.cepi.kstom.util.intersectAny
import world.cepi.kstom.util.playSound
import java.text.DecimalFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class LazerTagGame(gameOptions: GameOptions) : PvpGame(gameOptions) {

    companion object {
        val killsTag = Tag.Integer("kills")
        val killstreakTag = Tag.Integer("killstreak")
        val deathsTag = Tag.Integer("deaths")
    }

    val cancelKillstreakTasks = hashMapOf<Player, Task>()
    val respawnTasks = mutableListOf<Task>()
    val reloadTasks = hashMapOf<Player, Task>()

    override fun playerJoin(player: Player) {
        player.gameMode = GameMode.SPECTATOR
        player.isInvisible = false

        scoreboard?.createLine(
            Sidebar.ScoreboardLine(
                player.uuid.toString(),

                Component.text()
                    .append(Component.text(player.username, NamedTextColor.GRAY))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(0, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                    .build(),

                0
            )
        )

        player.setTag(killsTag, 0)
        player.setTag(killstreakTag, 0)
        player.setTag(deathsTag, 0)

        player.respawnPoint = getRandomRespawnPosition()

    }


    override fun playerLeave(player: Player) {
        player.isInvisible = false

        scoreboard?.removeLine(player.uuid.toString())
    }

    override fun gameStarted() {
        gameState = GameState.PLAYING

        players.forEach(::respawn)
    }

    override fun playerDied(player: Player, killer: Entity?) {
        if (gameState == GameState.ENDING) return

        player.inventory.clear()
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
        reloadTasks.remove(player)

        if (killer != null && killer != player && killer is Player) {
            val currentKills = killer.getTag(killsTag)!! + 1

            if (currentKills >= 20) return victory(killer)

            killer.setTag(killsTag, currentKills)

            scoreboard?.updateLineScore(killer.uuid.toString(), currentKills)
            scoreboard?.updateLineContent(
                killer.uuid.toString(),
                Component.text()
                    .append(Component.text(killer.username, NamedTextColor.GRAY))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(
                        Component.text(
                            currentKills,
                            TextColor.lerp(currentKills / 20f, NamedTextColor.GRAY, NamedTextColor.LIGHT_PURPLE),
                            TextDecoration.BOLD
                        )
                    )
                    .build()
            )

            sendMessage(
                Component.text()
                    .append(Component.text("☠", NamedTextColor.RED))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(killer.username, NamedTextColor.WHITE))
                    .append(Component.text(" killed ", NamedTextColor.GRAY))
                    .append(Component.text(player.username, NamedTextColor.RED))
                    .append(Component.text(" with ", NamedTextColor.GRAY))
                    .append(Component.text(killer.heldGun?.name ?: "air", NamedTextColor.GOLD))
            )

            killer.showTitle(
                Title.title(
                    Component.empty(),
                    Component.text()
                        .append(Component.text("☠ ", NamedTextColor.RED))
                        .append(Component.text(player.username, NamedTextColor.RED))
                        .build(),
                    Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500))
                )
            )

            killer.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.PLAYER, 1f, 1f))

            player.showTitle(
                Title.title(
                    Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                    "<gray>Killed by <red><bold>${killer.username}</bold></red>".asMini(),
                    Title.Times.of(
                        Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                    )
                )
            )

            val gun = Gun.registeredMap.values.random()
            killer.inventory.setItemStack(0, gun.item)
            gun.renderAmmo(killer, gun.ammo)
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
                    Title.Times.of(
                        Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                    )
                )
            )

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
                if (i <= 0) {
                    respawn(player)

                    cancel()
                    return
                }

                player.playSound(
                    Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f),
                    Sound.Emitter.self()
                )
                player.showTitle(
                    Title.title(
                        Component.text(i, NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.of(
                            Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                        )
                    )
                )

                i--
            }
        }.delay(Duration.ofSeconds(2)).repeat(Duration.ofSeconds(1)).schedule())
    }

    override fun respawn(player: Player) = with(player) {
        teleport(getRandomRespawnPosition())
        reset()

        player.playSound(
            Sound.sound(SoundEvent.BLOCK_BEACON_ACTIVATE, Sound.Source.MASTER, 1f, 1f),
            Sound.Emitter.self()
        )

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

        val victoryTitle = Title.title(
            Component.text("VICTORY!", NamedTextColor.GOLD, TextDecoration.BOLD),
            Component.text(EndGameQuotes.victory.random(), NamedTextColor.GRAY),
            Title.Times.of(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        )
        val defeatTitle = Title.title(
            Component.text("DEFEAT!", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(EndGameQuotes.defeat.random(), NamedTextColor.GRAY),
            Title.Times.of(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        )

        players.forEach {
            it.inventory.clear()

            if (it == player) it.showTitle(victoryTitle)
            else it.showTitle(defeatTitle)

        }

        Manager.scheduler.buildTask { destroy() }
            .delay(5, TimeUnit.SECOND)
            .schedule()
    }

    override fun gameDestroyed() {
        respawnTasks.forEach(Task::cancel)
        respawnTasks.clear()
    }

    override fun registerEvents() = with(eventNode) {
        listenOnly<PlayerUseItemEvent> {
            isCancelled = true
            if (hand != Player.Hand.MAIN) return@listenOnly

            val heldGun = player.heldGun ?: return@listenOnly
            if (player.itemInMainHand.meta.getTag(lastShotTag)!! > System.currentTimeMillis() - heldGun.cooldown) {
                return@listenOnly
            }
            if (player.itemInMainHand.meta.getTag(reloadingTag)!!.toInt() == 1) {
                return@listenOnly
            }

            player.itemInMainHand = player.itemInMainHand.and {
                setTag(lastShotTag, System.currentTimeMillis())
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
                        player.sendActionBar("<red>Press <bold><key:key.swapOffhand></bold> to reload!".asMini())

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
            if (entity.entityType == EntityType.PLAYER) return@listenOnly

            if (!entity.hasTag(Gun.playerUUIDTag)) return@listenOnly


            val shooter: Player = Manager.connection.getPlayer(UUID.fromString(entity.getTag(Gun.playerUUIDTag)))!!
            val gun = Gun.registeredMap[entity.getTag(Gun.gunIdTag)!!] ?: return@listenOnly

            if (gun !is ProjectileGun) return@listenOnly

            // TODO: Better collisions

            if (entity.velocity.x == 0.0 || entity.velocity.y == 0.0 || entity.velocity.z == 0.0) {
                gun.collide(shooter, entity)
                return@listenOnly
            }
            if (entity.aliveTicks > 20 * 3) {
                gun.collide(shooter, entity)
                return@listenOnly
            }

            val intersectingPlayers = players
                .filter { entity.boundingBox.intersect(it.boundingBox) && it.gameMode == GameMode.ADVENTURE }
                // TODO: Make shooter not collide for first second .filter { it != shooter &&  }
            if (intersectingPlayers.isEmpty()) return@listenOnly

            gun.collideEntity(shooter, entity, intersectingPlayers)
        }

        cancel<PlayerBlockBreakEvent>()
        cancel<InventoryPreClickEvent>()
        cancel<PlayerBlockBreakEvent>()
        cancel<PlayerBlockPlaceEvent>()
        cancel<ItemDropEvent>()

        listenOnly<PlayerChangeHeldSlotEvent> {
            isCancelled = true
            player.setHeldItemSlot(0)
        }

        listenOnly<PlayerSwapItemEvent> {
            isCancelled = true

            val gun = Gun.registeredMap[offHandItem.getTag(Gun.gunIdTag)] ?: return@listenOnly

            if (player.itemInMainHand.meta.getTag(reloadingTag)!!.toInt() == 1 || player.itemInMainHand.meta.getTag(
                    ammoTag
                ) == gun.ammo
            ) {
                return@listenOnly
            }

            player.itemInMainHand = player.itemInMainHand.withMeta<ItemMetaBuilder> {
                it.set(ammoTag, 0)
                it.set(reloadingTag, 1)
            }

            player.setCooldown(player.itemInMainHand.material, gun.reloadTime.toInt() / 50, true)

            reloadTasks[player] = object : MinestomRunnable() {
                var i = gun.ammo

                override fun run() {

                    if (i <= 0) {
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

                        player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
                            meta.set(ammoTag, gun.ammo)
                            meta.set(reloadingTag, 0)
                        }

                        gun.renderAmmo(player, gun.ammo - i)

                        cancel()
                        return
                    }

                    gun.renderAmmo(player, gun.ammo - i)
                    player.itemInMainHand = player.itemInMainHand.and {
                        this.set(ammoTag, gun.ammo - i)
                    }
                    player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 1f, 1f), Sound.Emitter.self())

                    i--
                }
            }.repeat(Duration.ofMillis(gun.reloadTime / gun.ammo)).schedule()
        }

        listenOnly<PlayerMoveEvent> {
            if (player.gameMode != GameMode.ADVENTURE) return@listenOnly

            if (newPosition.y() < 5) {
                kill(player, null)
            }
        }

        listenOnly<EntityDamageEvent> {
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

            val healthEntity = Entity(EntityType.ARMOR_STAND)
            val healthEntityMeta = healthEntity.entityMeta as ArmorStandMeta

            healthEntity.isAutoViewable = false
            healthEntity.isInvisible = true
            healthEntity.customName =
                Component.text()
                    .append(Component.text("❤ "))
                    .append(Component.text(format.format(damage), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .color(TextColor.lerp(damage / 20f, TextColor.color(255, 150, 150), NamedTextColor.DARK_RED))
                    .build()
            healthEntity.isCustomNameVisible = true
            healthEntity.setNoGravity(true)
            healthEntityMeta.setNotifyAboutChanges(false)
            healthEntityMeta.isMarker = true
            healthEntityMeta.isSmall = true
            healthEntityMeta.isHasNoBasePlate = true
            healthEntityMeta.setNotifyAboutChanges(true)

            healthEntity.setInstance(
                entity.instance!!,
                entity.position.sub(
                    rand.nextDouble(-0.5, 0.5),
                    rand.nextDouble(0.5) - 0.5,
                    rand.nextDouble(-0.5, 0.5)
                )
            )

            healthEntity.addViewer(damager)

            healthEntity.scheduleRemove(Duration.ofSeconds(2))

            object : MinestomRunnable() {
                var i = 1
                var accel = 0.5

                override fun run() {
                    if (i > 10) {
                        cancel()
                        return
                    }

                    player.sendMessage("$i")

                    healthEntity.teleport(healthEntity.position.add(0.0, accel, 0.0))
                    accel *= 0.60

                    i++
                }
            }.repeat(Duration.ofMillis(50 * 3)).schedule()
        }
    }

    private fun getRandomRespawnPosition(): Pos {
        return LazerTagExtension.config.spawnPositions["dizzymc"]?.random() ?: Pos(0.5, 10.0, 0.5)
    }

    override fun instanceCreate(): Instance {
        val instance = Manager.instance.createInstanceContainer(
            //Manager.dimensionType.getDimension(NamespaceID.from("fullbright"))!!
        )
        instance.chunkLoader = AnvilLoader("./maps/lazertag/dizzymc")

        return instance
    }

}