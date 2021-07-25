package emortal.lazertag

import emortal.immortal.game.GameManager
import emortal.immortal.game.GameOptions
import emortal.immortal.game.GameTypeInfo
import emortal.lazertag.commands.NewInstanceCommand
import emortal.lazertag.commands.SaveInstanceCommand
import emortal.lazertag.game.LazerTagGame
import emortal.lazertag.maps.MapManager
import net.minestom.server.extensions.Extension
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.command.register
import world.cepi.kstom.command.unregister

class LazerTagExtension : Extension() {

    override fun initialize() {

        MapManager.init(this)
        GameManager.registerGame<LazerTagGame>(
            GameTypeInfo(
                eventNode,
                "lazertag",
                "<gradient:gold:yellow><bold>LazerTag".asMini(),
                GameOptions(
                    setOf(MapManager.mapMap["dizzymc"]!!),
                    15,
                    2,
                    true
                )
            )
        )


        SaveInstanceCommand.register()
        NewInstanceCommand.register()

        logger.info("[LazerTagExtension] has been enabled!")
    }

    override fun terminate() {

        SaveInstanceCommand.unregister()
        NewInstanceCommand.unregister()

        logger.info("[LazerTagExtension] has been disabled!")
    }

}