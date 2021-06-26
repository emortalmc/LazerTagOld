package emortal.lazertag.game

import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object GameManager {
    private val gameMap: ConcurrentHashMap<UUID, Game> = ConcurrentHashMap<UUID, Game>()
    private val games: MutableSet<Game> = HashSet<Game>()
    private var gameIndex = 0

    /**
     * Adds a player to the game queue
     * @param player The player to add to the game queue
     */
    fun addPlayer(player: Player) {
        val game: Game = nextGame()
        game.addPlayer(player)
        gameMap[player.uuid] = game
    }

    fun removePlayer(player: Player) {
        getPlayerGame(player)
    }

    private fun createGame(): Game {
        val newGame = Game(gameIndex)
        games.add(newGame)
        gameIndex++
        return newGame
    }

    private fun nextGame(): Game {
        for (game in games) {
            if (game.gameState != GameState.STARTING) continue
            return game
        }
        return createGame()
    }

    fun getPlayerGame(player: Player): Game? {
        return gameMap[player.uuid]
    }

}