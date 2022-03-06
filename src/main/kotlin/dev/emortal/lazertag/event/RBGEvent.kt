package dev.emortal.lazertag.event

import dev.emortal.lazertag.game.LazerTagGame
import dev.emortal.lazertag.gun.RBG
import dev.emortal.lazertag.gun.Rifle
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.sound.SoundEvent
import java.time.Duration

class RBGEvent : Event() {

    override val duration = Duration.ofSeconds(20)

    override val startMessage = Component.text()
        .append(Component.text("Uh oh...", NamedTextColor.RED))
        .append(Component.text(" prepare for", NamedTextColor.GRAY))
        .append(Component.text(" lots ", NamedTextColor.GRAY, TextDecoration.ITALIC))
        .append(Component.text("of explosions; ", NamedTextColor.GRAY))
        .append(Component.text("the RBG event just started", NamedTextColor.YELLOW))
        .append(Component.text("!", NamedTextColor.GRAY))
        .build()

    override fun eventStarted(game: LazerTagGame) {
        game.playSound(Sound.sound(SoundEvent.ENTITY_ENDER_DRAGON_GROWL, Sound.Source.MASTER, 0.6f, 1f))

        game.defaultGun = RBG
        game.gunRandomizing = false
        game.players.forEach {
            game.setGun(it, RBG)
        }
    }

    override fun eventEnded(game: LazerTagGame) {
        game.playSound(Sound.sound(SoundEvent.ENTITY_BEE_DEATH, Sound.Source.MASTER, 0.6f, 0.4f))

        game.defaultGun = Rifle
        game.gunRandomizing = true
        game.players.forEach {
            game.setGun(it)
        }
    }

}