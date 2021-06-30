package emortal.lazertag.gun

import emortal.lazertag.utils.PlayerUtils.playSound
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.ItemStackBuilder
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag

abstract class Gun(val name: String, val id: Int) {

    companion object {
        val registeredMap = HashMap<Int, Gun>()
    }

    init {
        if (registeredMap.containsKey(id)) {
            println("Duplicate gun IDs")
        }
        registeredMap[id] = this
    }

    open val itemBuilder: ItemStackBuilder = ItemStack.builder(Material.WOODEN_HOE)
        .displayName(Component.text(name))
        .meta { meta: ItemMetaBuilder ->
            meta.set(Tag.Long("lastShot"), 0)
            meta.customModelData(id)
        }

    val item by lazy { itemBuilder.build() }
    val maxDurability by lazy { item.meta.damage }

    open val damage: Float = 1f // PER BULLET!
    open val numberOfBullets: Int = 1
    open val spread: Double = 0.0
    open val cooldown: Long = 1 // In millis
    open val ammo: Int = 10
    open val maxDistance: Double = 10.0

    open val burstAmount: Int = 0
    open val burstInterval: Long = 0 // In ticks

    open fun shoot(player: Player) {
        player.instance!!.playSound(Sound.sound(SoundEvent.BLAZE_HURT, Sound.Source.PLAYER, 1f, 1f), player.position)
    }
}