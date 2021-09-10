package emortal.lazertag

import emortal.immortal.game.GameManager
import emortal.immortal.game.GameOptions
import emortal.immortal.game.GameTypeInfo
import emortal.lazertag.commands.GunCommand
import emortal.lazertag.game.LazerTagGame
import emortal.lazertag.maps.MapManager
import net.minestom.server.extensions.Extension
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.command.register
import world.cepi.kstom.command.unregister

class LazerTagExtension : Extension() {



    override fun initialize() {
        GunCommand.register()

        MapManager.init(this)
        GameManager.registerGame<LazerTagGame>(
            GameTypeInfo(
                eventNode,
                "lazertag",
                "<gradient:gold:yellow><bold>LazerTag".asMini(),
                true,
                GameOptions(
                    { MapManager.mapMap["dizzymc"]!! },
                    15,
                    1,
                    true
                )
            )
        )

        logger.info("[LazerTagExtension] has been enabled!")
    }

    override fun terminate() {
        GunCommand.unregister()

        logger.info("[LazerTagExtension] has been disabled!")
    }

}