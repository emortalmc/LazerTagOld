package emortal.lazertag

import emortal.lazertag.commands.LazerTagCommand
import net.minestom.server.extensions.Extension
import world.cepi.kstom.Manager

class LazerTagExtension : Extension() {

    override fun initialize() {

        EventListener.init(this)

        Manager.command.register(LazerTagCommand)

        logger.info("[LazerTagExtension] has been enabled!")
    }

    override fun terminate() {
        Manager.command.unregister(LazerTagCommand)

        logger.info("[LazerTagExtension] has been disabled!")
    }

}