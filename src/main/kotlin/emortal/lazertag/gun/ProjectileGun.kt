package emortal.lazertag.gun

import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMetaBuilder

sealed class ProjectileGun(name: String, customMeta: (ItemMetaBuilder) -> Unit = {}) : Gun(name, customMeta) {

    abstract fun collide(shooter: Player, projectile: Entity)
    open fun collideEntity(shooter: Player, projectile: Entity, hitPlayers: Collection<Player>) {
        collide(shooter, projectile)
    }

}