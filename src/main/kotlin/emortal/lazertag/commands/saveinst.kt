package emortal.lazertag.commands

import emortal.lazertag.MapManager
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor

object saveinst : Command("saveinstance") {
    init {
        defaultExecutor =
            CommandExecutor { sender: CommandSender, context: CommandContext ->
                val player = sender.asPlayer()
                val instance = player.instance!!

                player.sendMessage("Re-placing spawn blocks...")
                MapManager.spawnPosBlocks[instance]?.forEach { (pos, material) ->
                    instance.setBlock(pos, material)
                }

                player.sendMessage("Saving instance...")
                instance.saveChunksToStorage {
                    player.sendMessage("Saved instance!")
                }
            }
    }
}