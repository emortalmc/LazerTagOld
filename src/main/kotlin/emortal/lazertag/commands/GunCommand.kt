package emortal.lazertag.commands

import emortal.lazertag.gun.Gun
import emortal.lazertag.gun.Gun.Companion.heldGun
import emortal.lazertag.gun.ProjectileGun
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand

object GunCommand : Kommand({
    val gunArg = ArgumentType.StringArray("gun").suggest {
        Gun.registeredMap.keys.toList()
    }

    syntax(gunArg) {
        val gun = context.get(gunArg).joinToString(separator = " ")
        player.heldGun = Gun.registeredMap[gun]
    }
}, "gun")