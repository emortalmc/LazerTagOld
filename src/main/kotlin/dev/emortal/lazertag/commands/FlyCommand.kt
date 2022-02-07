package dev.emortal.lazertag.commands

import dev.emortal.immortal.util.PermissionUtils.hasLuckPermission
import dev.emortal.lazertag.gun.Gun
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand

object FlyCommand : Kommand({
    onlyPlayers

    val gunArg = ArgumentType.StringArray("gun").suggest {
        Gun.registeredMap.keys.toList()
    }

    syntax(gunArg) {
        if (!player.hasLuckPermission("lazertag.fly")) {
            return@syntax
        }

        player.isAllowFlying = !player.isAllowFlying
    }
}, "lazertagfly")