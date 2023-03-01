package dev.emortal.lazertag.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.lazertag.game.LazerTagGame
import dev.emortal.lazertag.gun.Gun
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.Player

object GunCommand : Command("gun") {
    init {
        val gunArg = ArgumentType.StringArray("gun").setSuggestionCallback { _, _, suggestion ->
            Gun.registeredMap.keys.forEach {
                suggestion.addEntry(SuggestionEntry(it))
            }
        }

        addConditionalSyntax({ sender, _ ->
            sender.hasLuckPermission("lazertag.gun")
        }, { sender, ctx ->
            val player = sender as? Player ?: return@addConditionalSyntax

            val gunName = ctx.get(gunArg).joinToString(separator = " ")
            val gun = Gun.registeredMap[gunName] ?: return@addConditionalSyntax
            (player.game as LazerTagGame).setGun(player, gun)
        }, gunArg)

    }
}