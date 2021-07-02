package emortal.lazertag.commands

import emortal.lazertag.game.GameManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor

object LazerTagCommand : Command("lazertag") {
    init {
        defaultExecutor =
            CommandExecutor { sender: CommandSender, context: CommandContext ->
                GameManager.addPlayer(sender.asPlayer())
                sender.sendMessage(
                    Component.text(
                        "Joining a game of LazerTag...",
                        NamedTextColor.GREEN
                    )
                )
            }
    }
}