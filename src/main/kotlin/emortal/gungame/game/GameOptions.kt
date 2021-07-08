package emortal.gungame.game

data class GameOptions(
    val gameType: GameType = GameType.STANDARD,
    val map: String = "dizzymc",
    val teamType: TeamType = TeamType.TWO_TEAMS,
    val maxPlayers: Int = 15,
    val playersToStart: Int = 2
)