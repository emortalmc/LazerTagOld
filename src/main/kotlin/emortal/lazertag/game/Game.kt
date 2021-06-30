package emortal.lazertag.game

import emortal.lazertag.MapManager
import emortal.lazertag.utils.MinestomRunnable
import emortal.lazertag.utils.RandomUtils
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.timer.Task
import net.minestom.server.utils.Position
import net.minestom.server.utils.time.TimeUnit
import java.time.Duration

class Game(val id: Int, val options: GameOptions) {
    private val mini: MiniMessage = MiniMessage.get()

    val instance: Instance = MapManager.mapMap[options.map]!!

    val players: MutableSet<Player> = HashSet()
    val playerAudience: Audience = Audience.audience(players)
    var gameState: GameState = GameState.WAITING_FOR_PLAYERS

    // TODO: Maps
    private var startingTask: Task? = null
    private val victoryTask = MinecraftServer.getSchedulerManager().buildTask { destroy() }
        .delay(5, TimeUnit.SECOND)

    private val respawnTasks: List<Task> = ArrayList()

    fun addPlayer(player: Player) {
        players.add(player)
        playerAudience.sendMessage(mini.parse("<gray>[<green><bold>+</bold></green>] <white>" + player.username))

        player.respawnPoint = RandomUtils.ZERO_POS
        player.setInstance(instance)

        if (players.size == MAX_PLAYERS) {
            start()
            return
        }

        if (players.size == PLAYERS_TO_START) {
            gameState = GameState.STARTING

            startingTask = object : MinestomRunnable() {
                var secs = STARTING_SECONDS

                override fun run() {
                    if (secs < 1) {
                        cancel()
                        start()
                        return
                    }

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
        startingTask = null
        gameState = GameState.PLAYING
        for (player in players) {
            respawn(player)
        }
    }

    fun died(player: Player) {
        // Do some stuff
        // respawnTask.add()

        respawn(player)
    }
    private fun respawn(player: Player) {
        player.inventory.clear()
        player.teleport(getRandomRespawnPosition())
    }

    private fun getRandomRespawnPosition(): Position {
        return MapManager.spawnPositionMap[options.map]!!.random()
    }

    private fun victory() {

        // brain rescheduling
        victoryTask.schedule()
    }

    private fun destroy() {
        for (player in players) {
            GameManager.addPlayer(player)
        }
        players.clear()
    }

    companion object {
        const val MAX_PLAYERS = 8
        const val PLAYERS_TO_START = 2
        const val STARTING_SECONDS = 5
    }
}