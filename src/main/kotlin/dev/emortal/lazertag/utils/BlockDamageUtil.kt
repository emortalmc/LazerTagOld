package dev.emortal.lazertag.utils

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.BlockBreakAnimationPacket
import net.minestom.server.utils.PacketUtils
import java.util.concurrent.ThreadLocalRandom

fun Instance.sendBlockDamage(destroyStage: Byte, point: Point) {
    val packet = BlockBreakAnimationPacket(ThreadLocalRandom.current().nextInt(1000), point, destroyStage)
    PacketUtils.sendGroupedPacket(this.players, packet)
}