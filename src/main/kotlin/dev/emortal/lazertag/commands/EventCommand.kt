package dev.emortal.lazertag.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.lazertag.event.Event
import dev.emortal.lazertag.game.LazerTagGame
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.Player
import kotlin.reflect.full.primaryConstructor

object EventCommand : Command("event") {
    init {
        val eventArg = ArgumentType.StringArray("event").setSuggestionCallback { _, _, suggestion ->
            Event::class.sealedSubclasses.mapNotNull { it.simpleName }.forEach {
                suggestion.addEntry(SuggestionEntry(it))
            }
        }

        addConditionalSyntax({ sender, _ ->
            sender.hasLuckPermission("lazertag.event")
        }, { sender, ctx ->
            val player = sender as? Player ?: return@addConditionalSyntax

            val eventName = ctx.get(eventArg).joinToString(separator = " ")
            val event =
                Event::class.sealedSubclasses.firstOrNull { it.simpleName == eventName }?.primaryConstructor?.call()
                    ?: return@addConditionalSyntax

            event.performEvent(player.game as LazerTagGame)
        }, eventArg)

    }
}