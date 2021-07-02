package emortal.lazertag.game

import emortal.lazertag.items.ItemManager
import emortal.lazertag.utils.MinestomRunnable
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import net.minestom.server.utils.Position
import net.minestom.server.utils.Vector
import net.minestom.server.utils.time.TimeUnit
import java.time.Duration
import kotlin.properties.Delegates

class Game(val id: Int, val options: GameOptions) {
    private val mini: MiniMessage = MiniMessage.get()

    val instance: Instance = MapManager.mapMap[options.map]!!

    val players: MutableSet<Player> = HashSet()

    val playerAudience: Audience = Audience.audience(players)
    var gameState: GameState = GameState.WAITING_FOR_PLAYERS

    // TODO: Maps
    private var startingTask: Task? = null

    private val respawnTasks: ArrayList<Task> = ArrayList()

    var startTime by Delegates.notNull<Long>()

    fun addPlayer(player: Player) {
        players.add(player)
        playerAudience.sendMessage(mini.parse("<gray>[<green><bold>+</bold></green>] <white>" + player.username))

        player.setTag(Tag.Integer("kills"), 0)

        player.respawnPoint = getRandomRespawnPosition()
        if (player.instance!! != instance) player.setInstance(instance)
        player.gameMode = GameMode.SPECTATOR

        if (gameState == GameState.PLAYING) {
            respawn(player)
            return
        }

        if (players.size == options.maxPlayers) {
            start()
            return
        }

        if (players.size >= options.playersToStart) {
            if (startingTask != null) return

            gameState = GameState.STARTING

            startingTask = object : MinestomRunnable() {
                var secs = 5

                override fun run() {
                    if (secs < 1) {
                        cancel()
                        start()
                        return
                    }

                    playerAudience.playSound(Sound.sound(SoundEvent.WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f))
                    playerAudience.showTitle(
                        Title.title(
                            Component.text(secs, NamedTextColor.GREEN, TextDecoration.BOLD),
                            Component.empty(),
                            Title.Times.of(
                                Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                            )
                        )
                    )

                    secs--
                }
            }.repeat(1, TimeUnit.SECOND).schedule()
        }
    }

    fun removePlayer(player: Player) {
        players.remove(player)
        playerAudience.sendMessage(mini.parse("<gray>[<red><bold>-</bold></red>] <white>" + player.username))
    }

    private fun start() {
        startTime = System.currentTimeMillis()

        startingTask = null
        gameState = GameState.PLAYING

        for (player in players) {
            respawn(player)
        }

    }

    fun died(player: Player, killer: Player?, reason: DeathReason) {
        player.inventory.clear()
        player.velocity = Vector(0.0, 0.0, 0.0)
        player.gameMode = GameMode.SPECTATOR
        player.isInvisible = true

        if (killer != null) {
            val currentKills = killer.getTag(Tag.Integer("kills"))!! + 1

            if (currentKills >= 3) return victory(player)

            killer.setTag(Tag.Integer("kills"), currentKills)
            killer.sendMessage(mini.parse("<gray>You killed <white><bold>${player.username}</bold></white> Kills: $currentKills!"))
            killer.playSound(
                Sound.sound(
                    SoundEvent.NOTE_BLOCK_PLING,
                    Sound.Source.PLAYER,
                    1f,
                    1f
                )
            )

            player.spectate(killer)
        }



        player.showTitle(Title.title(
            Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
            mini.parse("<gray>Killed by " + if (reason == DeathReason.PLAYER && killer != null) "<white><bold>${killer.username}</bold></white>" else "<white><bold>the void</bold></white>"),
            Title.Times.of(
                Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
            )
        ))

        respawnTasks.add(object : MinestomRunnable() {
            var i = 3

            override fun run() {
                if (i <= 0) {
                    respawn(player)

                    cancel()
                    return
                }

                player.playSound(Sound.sound(SoundEvent.WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f))
                player.showTitle(Title.title(
                    Component.text(i, NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.empty(),
                    Title.Times.of(
                        Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                    )
                ))

                i--
            }
        }.delay(2, TimeUnit.SECOND).repeat(1, TimeUnit.SECOND).schedule())
    }
    private fun respawn(player: Player) {
        player.inventory.clear()
        player.teleport(getRandomRespawnPosition())
        player.stopSpectating()
        player.isInvisible = false
        player.gameMode = GameMode.ADVENTURE

        // TODO: Replace with proper gun score system
        player.inventory.addItemStack(ItemManager.LAZER_MINIGUN.item)
        player.inventory.addItemStack(ItemManager.LAZER_SHOTGUN.item)
        player.inventory.addItemStack(ItemManager.RIFLE.item)
        player.inventory.addItemStack(ItemManager.RAILGUN.item)
    }

    private fun getRandomRespawnPosition(): Position {
        return MapManager.spawnPositionMap[options.map]!!.random()
    }

    private fun victory(player: Player) {
        if (gameState == GameState.ENDING) return
        gameState = GameState.ENDING

        val message = Component.text()
            .append(Component.text("VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("\n${player.username} won the game", NamedTextColor.WHITE))
            .build()

        playerAudience.sendMessage(message)

        MinecraftServer.getSchedulerManager().buildTask { destroy() }
            .delay(5, TimeUnit.SECOND).schedule()
    }

    private fun destroy() {
        GameManager.deleteGame(this)

        for (player in players) {
            GameManager.addPlayer(player)
        }
        players.clear()
    }

    fun isFull(): Boolean = players.size >= options.maxPlayers
}