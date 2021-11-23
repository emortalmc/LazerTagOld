package emortal.lazertag.utils

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.SetCooldownPacket
import net.minestom.server.utils.time.TimeUnit

fun Player.setCooldown(material: Material, ticks: Int, lagCompensation: Boolean = true) {
    val packet = SetCooldownPacket()
    packet.cooldownTicks = ticks
    packet.itemId = material.id()

    this.playerConnection.sendPacket(packet)

    if (!lagCompensation) return

    MinecraftServer.getSchedulerManager().buildTask {
        val lagCompensatePacket = SetCooldownPacket()
        lagCompensatePacket.cooldownTicks = 0
        lagCompensatePacket.itemId = material.id()

        this.playerConnection.sendPacket(lagCompensatePacket)
    }.delay(ticks.toLong(), TimeUnit.CLIENT_TICK).schedule()
}