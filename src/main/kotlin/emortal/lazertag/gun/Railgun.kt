package emortal.lazertag.gun

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.ItemStackBuilder
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag

class Railgun : Gun("Railgun", 4) {

    override val itemBuilder: ItemStackBuilder = ItemStack.builder(Material.SPYGLASS)
        .displayName(Component.text(name))
        .meta { meta: ItemMetaBuilder ->
            meta.set(Tag.Long("lastShot"), 0)
            meta.customModelData(id)
        }

    override val damage = 20f
    override val numberOfBullets = 1
    override val cooldown = 100L
    override val ammo = 5
    override val maxDistance = 100.0

    override val sound = Sound.sound(SoundEvent.BEACON_ACTIVATE, Sound.Source.PLAYER, 1f, 1f)

}