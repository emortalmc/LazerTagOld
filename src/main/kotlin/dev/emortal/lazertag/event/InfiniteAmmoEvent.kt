package dev.emortal.lazertag.event

import dev.emortal.lazertag.game.LazerTagGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.sound.SoundEvent
import java.time.Duration

class InfiniteAmmoEvent : Event() {

    override val duration = Duration.ofSeconds(30)

    override val startMessage = Component.text()
        .append(Component.text("Abracadabra!", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
        .append(Component.text(" A rogue wizard casted a spell on your gun. ", NamedTextColor.GRAY))
        .append(Component.text("Infinite ammo has been enabled", NamedTextColor.YELLOW))
        .append(Component.text("!", NamedTextColor.GRAY))
        .build()

    override fun eventStarted(game: LazerTagGame) {
        game.playSound(Sound.sound(SoundEvent.ENTITY_WITCH_AMBIENT, Sound.Source.MASTER, 0.75f, 1f))
        game.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 0.75f, 1.2f))

        game.infiniteAmmo = true
    }

    override fun eventEnded(game: LazerTagGame) {
        game.playSound(Sound.sound(SoundEvent.ENTITY_WITCH_DEATH, Sound.Source.MASTER, 0.5f, 1f))

        game.infiniteAmmo = false
    }

}