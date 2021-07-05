package emortal.lazertag.game

import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.TeamsPacket
import world.cepi.kstom.Manager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object GameManager {
    private val gameMap: ConcurrentHashMap<Player, Game> = ConcurrentHashMap<Player, Game>()
    private val games: MutableSet<Game> = HashSet<Game>()
    private var gameIndex = 0

    // TODO: Use TeamType
    val team = Manager.team.createBuilder("team").nameTagVisibility(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS)

    /**
     * Adds a player to the game queue
     * @param player The player to add to the game queue
     */
    fun addPlayer(player: Player): Game {
        val game: Game = nextGame()

        game.addPlayer(player)
        gameMap[player] = game

        return game
    }

    fun removePlayer(player: Player) {
        this[player]?.removePlayer(player)

        gameMap.remove(player)
    }

    fun createGame(options: GameOptions): Game {
        val newGame = Game(gameIndex, options)
        games.add(newGame)
        gameIndex++
        return newGame
    }
    
    fun deleteGame(game: Game) {
        game.players.forEach(gameMap::remove)
        games.remove(game)
    }
    

    fun nextGame(): Game {
        for (game in games) {
            if (game.gameState == GameState.ENDING) continue
            if (game.isFull()) continue

            return game
        }
        return createGame(GameOptions())
    }

    operator fun get(player: Player) =
        gameMap[player]

}