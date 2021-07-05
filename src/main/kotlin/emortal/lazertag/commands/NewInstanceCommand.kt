package emortal.lazertag.commands

import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor
import world.cepi.kstom.Manager

object NewInstanceCommand : Command("newinstance") {
    init {
        defaultExecutor =
            CommandExecutor { sender: CommandSender, context: CommandContext ->
                val player = sender.asPlayer()
                val storageLocation = Manager.storage.getLocation("dizzymc")
                val instance = Manager.instance.createInstanceContainer(storageLocation)
                instance.chunkGenerator = VoidGenerator

                player.sendMessage("Created instance")

                player.setInstance(instance)
                player.isAllowFlying = true
                player.isFlying = true
            }
    }
}