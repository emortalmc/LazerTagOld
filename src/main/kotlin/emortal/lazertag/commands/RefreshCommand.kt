package emortal.lazertag.commands

import emortal.lazertag.LazerTagExtension
import emortal.lazertag.config.ConfigurationHelper
import emortal.lazertag.config.LazerTagConfig
import net.minestom.server.coordinate.Pos
import world.cepi.kstom.command.kommand.Kommand

object RefreshCommand : Kommand({
    default {
        LazerTagExtension.config = ConfigurationHelper.initConfigFile(LazerTagExtension.configPath, LazerTagConfig())

        LazerTagExtension.maps.forEach {
            if (LazerTagExtension.config.spawnPositions.contains(it)) return@forEach
            LazerTagExtension.config.spawnPositions[it] = arrayOf(Pos.ZERO)

            sender.sendMessage("Creating map config for '${it}'")
        }

        ConfigurationHelper.writeObjectToPath(LazerTagExtension.configPath, LazerTagExtension.config)

        sender.sendMessage("Refreshed spawn locations")
    }
}, "refresh-lazertag")