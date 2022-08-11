package dev.emortal.lazertag.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.lazertag.game.LazerTagGame
import dev.emortal.lazertag.gun.Gun
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand

object GunCommand : Kommand({
    onlyPlayers()

    val gunArg = ArgumentType.StringArray("gun").suggest {
        Gun.registeredMap.keys.toList()
    }

    syntax(gunArg) {
        if (!player.hasLuckPermission("lazertag.gun")) {
            return@syntax
        }

        val gun = context.get(gunArg).joinToString(separator = " ")
        val gunobject = Gun.registeredMap[gun] ?: return@syntax
        (player.game as LazerTagGame).setGun(player, gunobject)
    }
}, "gun")