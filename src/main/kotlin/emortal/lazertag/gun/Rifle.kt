package emortal.lazertag.gun

import net.kyori.adventure.text.Component
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.ItemStackBuilder
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag

class Rifle : Gun("Rifle", 3) {

    override val itemBuilder: ItemStackBuilder = ItemStack.builder(Material.WOODEN_HOE)
        .displayName(Component.text(name))
        .meta { meta: ItemMetaBuilder ->
            meta.set(Tag.Long("lastShot"), 0)
            meta.customModelData(id)
        }

    override val damage = 10f
    override val numberOfBullets = 1
    override val cooldown = 350L
    override val ammo = 40
    override val maxDistance = 75.0

}