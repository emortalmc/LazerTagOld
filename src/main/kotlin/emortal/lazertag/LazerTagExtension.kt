package emortal.lazertag

import emortal.lazertag.commands.LazerTagCommand
import emortal.lazertag.commands.newinst
import emortal.lazertag.commands.saveinst
import emortal.lazertag.game.MapManager
import net.minestom.server.extensions.Extension
import world.cepi.kstom.Manager

class LazerTagExtension : Extension() {

    override fun initialize() {

        EventListener.init(this)
        MapManager.init()

        Manager.command.register(LazerTagCommand)
        Manager.command.register(saveinst)
        Manager.command.register(newinst)

        logger.info("[LazerTagExtension] has been enabled!")
    }

    override fun terminate() {

        logger.info("[LazerTagExtension] has been disabled!")
    }

}