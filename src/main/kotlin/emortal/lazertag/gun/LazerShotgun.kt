package emortal.lazertag.gun

import net.kyori.adventure.text.Component
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class LazerShotgun : Gun("Lazer Shotgun", 2) {

    override val item: ItemStack = ItemStack.builder(Material.WOODEN_HOE)
        .displayName(Component.text(name))
        .meta { itemMetaBuilder: ItemMetaBuilder ->
            itemMetaBuilder.customModelData(id)
        }
        .build()

    override val damage = 5

    override val numberOfBullets: Int = 10

    override val spread: Double = 1.0

    override val type = GunType.GUN

}