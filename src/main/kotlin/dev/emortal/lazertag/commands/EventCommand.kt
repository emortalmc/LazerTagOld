package dev.emortal.lazertag.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.lazertag.event.Event
import dev.emortal.lazertag.game.LazerTagGame
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand
import kotlin.reflect.full.primaryConstructor

object EventCommand : Kommand({
    onlyPlayers

    val eventArg = ArgumentType.StringArray("event").suggest {
        Event::class.sealedSubclasses.mapNotNull { it.simpleName }
    }

    syntax(eventArg) {
        if (!player.hasLuckPermission("lazertag.event")) {
            return@syntax
        }

        val event = context.get(eventArg).joinToString(separator = " ")
        val eventObject =
            Event::class.sealedSubclasses.firstOrNull { it.simpleName == event }?.primaryConstructor?.call()
                ?: return@syntax

        eventObject.performEvent(player.game as LazerTagGame)

    }
}, "event")