package emortal.lazertag.gun

import net.kyori.adventure.text.Component
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.ItemStackBuilder
import net.minestom.server.item.Material

abstract class Gun(val name: String, val id: Int) {

    companion object {
        val registeredMap = HashMap<Int, Gun>()
    }

    init {
        registeredMap[id] = this
    }

    open val itemBuilder: ItemStackBuilder = ItemStack.builder(Material.WOODEN_HOE)
        .displayName(Component.text(name))
        .meta { itemMetaBuilder: ItemMetaBuilder ->
            itemMetaBuilder.customModelData(id)
        }

    val item by lazy { itemBuilder.build() }

    abstract val damage: Float
    abstract val numberOfBullets: Int
    abstract val spread: Double
    abstract val type: GunType
    abstract val cooldown: Int // In ticks
}