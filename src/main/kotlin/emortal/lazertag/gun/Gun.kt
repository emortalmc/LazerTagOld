package emortal.lazertag.gun

import net.kyori.adventure.text.Component
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

abstract class Gun(val name: String, val id: Int) {
    open val item: ItemStack = ItemStack.builder(Material.WOODEN_HOE)
        .displayName(Component.text(name))
        .meta { itemMetaBuilder: ItemMetaBuilder ->
            itemMetaBuilder.customModelData(id)
        }
        .build()

    abstract val damage: Int
    abstract val numberOfBullets: Int
    abstract val spread: Double
    abstract val type: GunType
}