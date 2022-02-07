package dev.emortal.lazertag.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.util.PermissionUtils.hasLuckPermission
import dev.emortal.lazertag.game.LazerTagGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.command.kommand.Kommand

object RGBCommand : Kommand({

    onlyPlayers

    default {
        if (!player.hasLuckPermission("lazertag.rbg")) {
            return@default
        }
        val game = player.game as LazerTagGame

        game.sendMessage(
            Component.text("uh oh, the game has been RBGed!!", NamedTextColor.RED)
        )
        game.gunRandomizing = false
        game.playSound(Sound.sound(SoundEvent.ENTITY_ENDER_DRAGON_GROWL, Sound.Source.MASTER, 1f, 1f))
        game.players.forEach {
            //game.setGun(it, RBG)
        }
    }

}, "rgbify")