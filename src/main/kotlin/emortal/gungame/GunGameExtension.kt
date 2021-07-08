package emortal.gungame

import emortal.gungame.commands.NewInstanceCommand
import emortal.gungame.commands.SaveInstanceCommand
import emortal.gungame.maps.MapManager
import net.minestom.server.extensions.Extension
import world.cepi.kstom.command.register
import world.cepi.kstom.command.unregister

class GunGameExtension : Extension() {

    override fun initialize() {

        EventListener.init(this)
        MapManager.init()

        SaveInstanceCommand.register()
        NewInstanceCommand.register()

        logger.info("[GunGameExtension] has been enabled!")
    }

    override fun terminate() {

        SaveInstanceCommand.unregister()
        NewInstanceCommand.unregister()

        logger.info("[GunGameExtension] has been disabled!")
    }

}