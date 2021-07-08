package emortal.gungame.game

enum class GameState {
    /** When the game is waiting for players to join */
    WAITING_FOR_PLAYERS,
    /** When the game is counting down */
    STARTING,
    /** When the game is in progress */
    PLAYING,
    /** When the victory screen is being shown, players will then be moved to another game */
    ENDING
}