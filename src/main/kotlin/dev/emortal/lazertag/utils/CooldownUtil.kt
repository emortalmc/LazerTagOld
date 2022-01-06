package dev.emortal.lazertag.utils

import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.SetCooldownPacket
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager

fun Player.setCooldown(material: Material, ticks: Int, lagCompensation: Boolean = true) {
    val packet = SetCooldownPacket(material.id(), ticks)

    this.sendPacket(packet)

    if (!lagCompensation) return

    Manager.scheduler.buildTask {
        val lagCompensatePacket = SetCooldownPacket(material.id(), 0)
        this.sendPacket(lagCompensatePacket)
    }.delay(ticks.toLong(), TimeUnit.CLIENT_TICK).schedule()
}