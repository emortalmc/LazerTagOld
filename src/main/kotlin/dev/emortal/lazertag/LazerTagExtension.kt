package dev.emortal.lazertag

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameOptions
import dev.emortal.immortal.game.WhenToRegisterEvents
import dev.emortal.lazertag.commands.GunCommand
import dev.emortal.lazertag.config.ConfigurationHelper
import dev.emortal.lazertag.config.LazerTagConfig
import dev.emortal.lazertag.game.LazerTagGame
import net.minestom.server.coordinate.Pos
import net.minestom.server.extensions.Extension
import world.cepi.kstom.adventure.asMini
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

class LazerTagExtension : Extension() {

    companion object {
        val configPath = Path.of("./lazertag.json")
        val mapsPath = Path.of("./maps/lazertag/").also { it.createDirectories() }
        val maps = mapsPath.listDirectoryEntries().map { it.nameWithoutExtension }

        var config: LazerTagConfig = ConfigurationHelper.initConfigFile(configPath, LazerTagConfig())
    }

    override fun initialize() {
        mapsPath.createDirectories()

        logger.info("Found ${maps.size} maps: \n- ${maps.joinToString("\n- ")}")

        maps.forEach {
            if (config.spawnPositions.contains(it)) return@forEach
            config.spawnPositions[it] = arrayOf(Pos.ZERO)

            logger.info("Creating map config for '${it}'")
        }

        ConfigurationHelper.writeObjectToPath(configPath, config)

        GameManager.registerGame<LazerTagGame>(
            eventNode,
            "lazertag",
            "<gradient:gold:yellow><bold>LazerTag".asMini(),
            showsInSlashPlay = true,
            canSpectate = true,
            WhenToRegisterEvents.GAME_START,
            GameOptions(
                maxPlayers = 15,
                minPlayers = 2,
                canJoinDuringGame = false,
                showScoreboard = true,
            )
        )

        GunCommand.register()
        //FlyCommand.register()
        //RGBCommand.register()

        logger.info("[LazerTag] has been enabled!")
    }

    override fun terminate() {
        GunCommand.unregister()
        //FlyCommand.unregister()
        //RGBCommand.unregister()

        logger.info("[LazerTag] has been disabled!")
    }

}