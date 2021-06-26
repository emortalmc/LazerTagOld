package emortal.lazertag.gun

import net.kyori.adventure.text.Component
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

abstract class Gun(val name: String) {
    open val item: ItemStack = ItemStack.builder(Material.WOODEN_HOE)
        .displayName(Component.text(name))
        .build()

    abstract val damage: Int
    abstract val numberOfBullets: Int
    abstract val spread: Double
    abstract val type: GunType
}