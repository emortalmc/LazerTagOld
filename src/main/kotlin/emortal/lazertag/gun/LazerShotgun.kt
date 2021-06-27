package emortal.lazertag.gun

import net.kyori.adventure.text.Component
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.ItemStackBuilder
import net.minestom.server.item.Material

class LazerShotgun : Gun("Lazer Shotgun", 2) {

    override val itemBuilder: ItemStackBuilder = ItemStack.builder(Material.WOODEN_HOE)
        .displayName(Component.text(name))
        .meta { itemMetaBuilder: ItemMetaBuilder ->
            itemMetaBuilder.customModelData(id)
        }

    override val damage = 1.5f // PER BULLET!

    override val numberOfBullets: Int = 20

    override val spread: Double = 0.15

    override val type = GunType.SHOTGUN

    override val cooldown = 12

}