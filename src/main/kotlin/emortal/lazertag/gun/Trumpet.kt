package emortal.lazertag.gun

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Player
import net.minestom.server.item.Material

object Trumpet : Gun("Trumpet", { it.customModelData(1) }) {

    override val material: Material = Material.PHANTOM_MEMBRANE

    override val color: TextColor = NamedTextColor.GRAY

    override val cooldown = 2000L
    override val ammo = 1

    override val sound = Sound.sound(Key.key("item.trumpet.doot"), Sound.Source.PLAYER, 1f, 1f)

    override fun shoot(player: Player): HashMap<Player, Float> {

        player.instance!!.players
            .filter { it.getDistance(player) < 8 && it != player }
            .forEach {
                val direction = it.position.sub(player.position).asVec()
                it.velocity = direction.mul(70.0).add(0.0, 10.0, 0.0)
            }

        return HashMap()
    }

}