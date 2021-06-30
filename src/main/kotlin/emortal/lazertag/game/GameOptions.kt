package emortal.lazertag.game

data class GameOptions(
    var gameType: GameType = GameType.STANDARD,
    var map: String = "dizzymc",
    var teamType: TeamType = TeamType.TWO_TEAMS
)