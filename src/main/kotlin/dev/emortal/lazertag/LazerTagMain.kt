package dev.emortal.lazertag

import dev.emortal.immortal.Immortal
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.game.GameManager
import dev.emortal.lazertag.LazerTagMain.Companion.config
import dev.emortal.lazertag.LazerTagMain.Companion.configPath
import dev.emortal.lazertag.LazerTagMain.Companion.maps
import dev.emortal.lazertag.LazerTagMain.Companion.mapsPath
import dev.emortal.lazertag.commands.EventCommand
import dev.emortal.lazertag.commands.GunCommand
import dev.emortal.lazertag.config.LazerTagConfig
import dev.emortal.lazertag.game.LazerTagGame
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

private val LOGGER = LoggerFactory.getLogger(LazerTagMain::class.java)

fun main() {
    Immortal.initAsServer()

    mapsPath.createDirectories()

    LOGGER.info("Found ${maps.size} maps: \n- ${maps.joinToString("\n- ")}")

    maps.forEach {
        if (config.spawnPositions.contains(it)) return@forEach
        config.spawnPositions[it] = arrayOf(Pos.ZERO)

        LOGGER.info("Creating map config for '${it}'")
    }

    ConfigHelper.writeObjectToPath(configPath, config)

    GameManager.registerGame<LazerTagGame>(
        "lazertag",
        MiniMessage.miniMessage().deserialize("<gradient:gold:yellow><bold>LazerTag"),
        showsInSlashPlay = true
    )

    val commandMgr = MinecraftServer.getCommandManager();
    commandMgr.register(GunCommand)
    commandMgr.register(EventCommand)
}

class LazerTagMain {
    companion object {
        val configPath = Path.of("./lazertag.json")
        val mapsPath = Path.of("./maps/lazertag/").also { it.createDirectories() }
        val maps = mapsPath.listDirectoryEntries().map { it.nameWithoutExtension }

        var config: LazerTagConfig = ConfigHelper.initConfigFile(configPath, LazerTagConfig())

        //lateinit var lazertagInstance: InstanceContainer
    }
}