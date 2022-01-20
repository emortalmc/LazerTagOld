package dev.emortal.lazertag.gun

import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.lazertag.game.LazerTagGame
import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMetaBuilder
import world.cepi.kstom.item.and
import world.cepi.kstom.util.playSound
import java.time.Duration

sealed class ProjectileGun(name: String, customMeta: (ItemMetaBuilder) -> Unit = {}) : Gun(name, customMeta) {

    open val maxDuration: Int = 5 * 20

    companion object {
        private val entityTaskMap = hashMapOf<Entity, MinestomRunnable>()
    }

    fun projectileTick(game: LazerTagGame, projectile: Entity, shooter: Player) {
        tick(game, projectile)

        if (projectile.velocity.x == 0.0 || projectile.velocity.y == 0.0 || projectile.velocity.z == 0.0) return collide(
            shooter,
            projectile
        )

        val intersectingPlayers = game.players
            .filter { it.gameMode == GameMode.ADVENTURE && projectile.boundingBox.intersect(it.boundingBox) }
            .filter { if (projectile.aliveTicks < 30) it != shooter else true }
        if (intersectingPlayers.isEmpty()) return

        collidedWithEntity(shooter, projectile, intersectingPlayers)
    }

    open fun tick(game: LazerTagGame, projectile: Entity) {}


    override fun shoot(game: LazerTagGame, player: Player): HashMap<Player, Float> {
        sound?.let { game.playSound(it, player.position) }

        val newAmmo = (player.itemInMainHand.meta.getTag(ammoTag) ?: 1) - 1
        renderAmmo(player, newAmmo)
        player.itemInMainHand = player.itemInMainHand.and {
            setTag(ammoTag, newAmmo)
        }

        repeat(numberOfBullets) {
            val entity = createEntity(player).also {
                //it.setTag(playerUUIDLeastTag, player.uuid.leastSignificantBits)
                //it.setTag(playerUUIDMostTag, player.uuid.mostSignificantBits)
                it.setTag(gunIdTag, this.name)
            }

            entityTaskMap[entity] =
                object :
                    MinestomRunnable(repeat = Duration.ofMillis(50), iterations = maxDuration, timer = game.timer) {
                    override fun run() {
                        projectileTick(game, entity, player)
                    }

                    override fun cancelled() {
                        entity.remove()
                        entityTaskMap.remove(entity)
                    }
                }
        }



        return projectileShot(game, player)
    }

    abstract fun projectileShot(game: LazerTagGame, player: Player): HashMap<Player, Float>

    fun collide(shooter: Player, projectile: Entity) {
        collided(shooter, projectile)

        entityTaskMap[projectile]?.cancel()
        projectile.remove()
    }

    fun collideEntity(shooter: Player, projectile: Entity, hitPlayers: Collection<Player>) {
        collidedWithEntity(shooter, projectile, hitPlayers)

        entityTaskMap[projectile]?.cancel()
        projectile.remove()
    }

    abstract fun collided(shooter: Player, projectile: Entity)
    open fun collidedWithEntity(shooter: Player, projectile: Entity, hitPlayers: Collection<Player>) {
        collided(shooter, projectile)
    }

    abstract fun createEntity(shooter: Player): Entity

}