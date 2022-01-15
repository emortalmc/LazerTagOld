package dev.emortal.lazertag.utils

import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.BlockBreakAnimationPacket
import net.minestom.server.network.packet.server.play.EffectPacket
import net.minestom.server.utils.PacketUtils
import java.util.concurrent.ThreadLocalRandom

fun PacketGroupingAudience.sendBlockDamage(destroyStage: Byte, point: Point) {
    val packet = BlockBreakAnimationPacket(ThreadLocalRandom.current().nextInt(1000), point, destroyStage)
    sendGroupedPacket(packet)
}

fun PacketGroupingAudience.breakBlock(point: Point, block: Block) {
    sendGroupedPacket(EffectPacket(2001/*Block break + block break sound*/, point, block.stateId().toInt(), false))
}