package emortal.lazertag.game

enum class GameState {
    WAITING_FOR_PLAYERS,  // When the game is waiting for players to join
    STARTING,  // When the game is counting down
    PLAYING,  // When the game is in progress
    ENDING // When the victory screen is being shown, players will then be moved to another game
}