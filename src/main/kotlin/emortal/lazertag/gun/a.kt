package emortal.lazertag.gun

import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class a {
    init {
        ItemStack.builder(Material.GLASS)
            .meta { itemMetaBuilder: ItemMetaBuilder -> itemMetaBuilder.customModelData(3) }
    }
}