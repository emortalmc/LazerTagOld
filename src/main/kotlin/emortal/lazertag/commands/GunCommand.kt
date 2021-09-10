package emortal.lazertag.commands

import emortal.lazertag.gun.Gun
import emortal.lazertag.gun.Gun.Companion.heldGun
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.command.addSyntax
import world.cepi.kstom.command.arguments.suggest

object GunCommand : Command("gun") {

    init {

        val gunArg = ArgumentType.StringArray("gun").suggest {
            Gun.registeredMap.keys.toList()
        }

        addSyntax(gunArg) {
            val player = sender.asPlayer()
            val gun = context.get(gunArg).joinToString(separator = " ")
            println(gun)
            player.heldGun = Gun.registeredMap[gun]
        }
    }

}