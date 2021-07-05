package emortal.lazertag.commands

import emortal.lazertag.maps.MapManager
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor

object SaveInstanceCommand : Command("saveinstance") {
    init {
        defaultExecutor =
            CommandExecutor { sender: CommandSender, context: CommandContext ->
                val player = sender.asPlayer()
                val instance = player.instance!!

                player.sendMessage("Saving instance...")
                MapManager.spawnPosBlocks[instance]?.forEach { (pos, material) ->
                    instance.setBlock(pos, material)
                }

                instance.saveChunksToStorage {
                    player.sendMessage("Saved instance!")
                }
            }
    }
}