package emortal.lazertag.gun

import net.kyori.adventure.text.Component
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.ItemStackBuilder
import net.minestom.server.item.Material

class LazerRifle : Gun("Lazer Rifle", 1) {

    override val itemBuilder: ItemStackBuilder = ItemStack.builder(Material.WOODEN_HOE)
        .displayName(Component.text(name))
        .meta { itemMetaBuilder: ItemMetaBuilder ->
            itemMetaBuilder.customModelData(id)
        }

    override val damage = 4f

    override val numberOfBullets: Int = 2

    override val spread: Double = 0.0

    override val type = GunType.GUN

    override val cooldown: Int = 7

}