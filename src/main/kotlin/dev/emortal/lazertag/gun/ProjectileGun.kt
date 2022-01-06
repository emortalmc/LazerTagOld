package dev.emortal.lazertag.gun

import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.timer.Task

sealed class ProjectileGun(name: String, customMeta: (ItemMetaBuilder) -> Unit = {}) : Gun(name, customMeta) {

    companion object {
        val entityTaskMap = hashMapOf<Entity, Task>()
    }

    abstract fun collide(shooter: Player, projectile: Entity)
    open fun collideEntity(shooter: Player, projectile: Entity, hitPlayers: Collection<Player>) {
        collide(shooter, projectile)
    }

}