package dev.emortal.lazertag.event

import dev.emortal.lazertag.game.LazerTagGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.attribute.Attribute
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.instance.Instance
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.sound.SoundEvent
import java.time.Duration

class SuperchargedEvent : Event() {

    override val duration = Duration.ofSeconds(30)

    override val startMessage = Component.text()
        .append(Component.text("Zap! ", NamedTextColor.AQUA, TextDecoration.BOLD))
        .append(Component.text("All players have been supercharged. ", NamedTextColor.GRAY))
        .append(Component.text("Running speed and jump height has been increased", NamedTextColor.YELLOW))
        .append(Component.text("!", NamedTextColor.GRAY))
        .build()

    override fun eventStarted(game: LazerTagGame) {
        game.players.forEach {
            strikeLightning(game.instance, it.position)

            it.getAttribute(Attribute.MOVEMENT_SPEED).baseValue = 0.3f
            it.addEffect(Potion(PotionEffect.JUMP_BOOST, 5, (duration.toMillis() / 50).toInt()))
        }
    }

    override fun eventEnded(game: LazerTagGame) {
        game.players.forEach {
            it.playSound(Sound.sound(SoundEvent.BLOCK_BEACON_DEACTIVATE, Sound.Source.MASTER, 1f, 1f))

            it.getAttribute(Attribute.MOVEMENT_SPEED).baseValue = 0.1f
            it.removeEffect(PotionEffect.JUMP_BOOST)
        }
    }

    private fun strikeLightning(instance: Instance, pos: Pos) {
        val entity = Entity(EntityType.LIGHTNING_BOLT)
        entity.setInstance(instance, pos)
        entity.scheduleRemove(Duration.ofSeconds(2))
    }

}