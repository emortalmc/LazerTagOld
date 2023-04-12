package dev.emortal.lazertag.gun

import dev.emortal.lazertag.game.LazerTagGame
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMeta
import net.minestom.server.timer.TaskSchedule

sealed class ProjectileGun(name: String, rarity: Rarity = Rarity.COMMON, customMeta: (ItemMeta.Builder) -> Unit = {}) : Gun(name, rarity, customMeta) {

    open fun tick(game: LazerTagGame, projectile: Entity, shooter: Player) {}

    override fun shoot(game: LazerTagGame, player: Player): Map<Player, Float> {
        sound?.let { game.playSound(it, player.position) }

        if (!game.infiniteAmmo) {
            val newAmmo = (player.itemInMainHand.meta().getTag(ammoTag) ?: 1) - 1
            renderAmmo(player, newAmmo)
            player.itemInMainHand = player.itemInMainHand.withMeta {
                it.set(ammoTag, newAmmo)
            }
        }

        repeat(numberOfBullets) {
            createEntity(player).also {
                it.setTag(shooterTag, player.uuid)
                it.setTag(gunIdTag, this.name)

                it.scheduler().buildTask {
                    tick(game, it, player)
                }.repeat(TaskSchedule.nextTick()).schedule()
            }
        }

        return projectileShot(game, player)
    }

    abstract fun projectileShot(game: LazerTagGame, player: Player): Map<Player, Float>

    fun collide(game: LazerTagGame, shooter: Player, projectile: Entity) {
        collided(game, shooter, projectile)
        projectile.remove()
    }

    fun collideEntity(game: LazerTagGame, shooter: Player, projectile: Entity, hitPlayers: Collection<Player>) {
        collidedWithEntity(game, shooter, projectile, hitPlayers)
        projectile.remove()
    }

    abstract fun collided(game: LazerTagGame, shooter: Player, projectile: Entity)
    open fun collidedWithEntity(
        game: LazerTagGame,
        shooter: Player,
        projectile: Entity,
        hitPlayers: Collection<Player>
    ) {
        collided(game, shooter, projectile)
    }

    abstract fun createEntity(shooter: Player): Entity

}