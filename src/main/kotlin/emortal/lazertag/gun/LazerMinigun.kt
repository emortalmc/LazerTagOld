package emortal.lazertag.gun

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.ItemStackBuilder
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag

class LazerMinigun : Gun("Lazer Minigun", 1) {

    override val itemBuilder: ItemStackBuilder = ItemStack.builder(Material.WOODEN_HOE)
        .displayName(Component.text(name))
        .meta { meta: ItemMetaBuilder ->
            meta.set(Tag.Long("lastShot"), 0)
            meta.customModelData(id)
        }

    override val damage = 1f
    override val numberOfBullets = 1
    override val cooldown = 150L
    override val ammo = 40
    override val maxDistance = 30.0

    override val burstAmount = 5
    override val burstInterval = 1L

    override val sound = Sound.sound(SoundEvent.ARMOR_STAND_HIT, Sound.Source.PLAYER, 1f, 1f)

}