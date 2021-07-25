package emortal.lazertag.commands

import emortal.lazertag.utils.VoidGenerator
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.instance.AnvilLoader
import world.cepi.kstom.Manager

object NewInstanceCommand : Command("newinstance") {
    init {
        defaultExecutor =
            CommandExecutor { sender: CommandSender, context: CommandContext ->
                val player = sender.asPlayer()
                val instance = Manager.instance.createInstanceContainer()
                instance.chunkGenerator = VoidGenerator
                instance.chunkLoader = AnvilLoader("dizzymc")

                player.sendMessage("Created instance")

                player.setInstance(instance)
                player.isAllowFlying = true
                player.isFlying = true
            }
    }
}