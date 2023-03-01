package dev.emortal.lazertag.utils

import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.SetCooldownPacket
import net.minestom.server.timer.TaskSchedule

fun Player.setCooldown(material: Material, ticks: Int, lagCompensation: Boolean = true) {
    this.sendPacket(SetCooldownPacket(material.id(), ticks))

    if (!lagCompensation) return

    this.scheduler().buildTask {
        this.sendPacket(SetCooldownPacket(material.id(), 0))
    }.delay(TaskSchedule.tick(ticks)).schedule()
}