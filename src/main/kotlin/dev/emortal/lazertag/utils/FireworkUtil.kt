package dev.emortal.lazertag.utils

import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.FireworkRocketMeta
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.metadata.FireworkMeta
import net.minestom.server.network.packet.server.play.EntityStatusPacket

/**
 * Creates a firework explode effect
 *
 * e.g. `player.showFirework(instance, player.position, mutableListOf(...))`
 *
 * @param instance The instance to explode in
 * @param position Where to explode
 * @param effects List of FireworkEffect
 */
fun PacketGroupingAudience.showFirework(
    instance: Instance,
    position: Pos,
    effects: MutableList<FireworkEffect>
) {
    val fireworkMeta = FireworkMeta.Builder().effects(effects).build()
    val fireworkItemStack = ItemStack.builder(Material.FIREWORK_ROCKET).meta(fireworkMeta).build()
    val firework = Entity(EntityType.FIREWORK_ROCKET)
    val meta = firework.entityMeta as FireworkRocketMeta

    meta.fireworkInfo = fireworkItemStack
    firework.setInstance(instance, position)

    sendGroupedPacket(EntityStatusPacket(firework.entityId, 17))

    firework.remove()
}
