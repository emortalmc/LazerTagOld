package dev.emortal.lazertag.gun

import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.lazertag.game.LazerTagGame
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMeta
import net.minestom.server.timer.TaskSchedule
import world.cepi.kstom.util.playSound
import java.util.concurrent.ConcurrentHashMap

sealed class ProjectileGun(name: String, rarity: Rarity = Rarity.COMMON, customMeta: (ItemMeta.Builder) -> Unit = {}) :
    Gun(name, rarity, customMeta) {

    open val maxDuration: Int = 5 * 20
    open val boundingBoxExpand: Vec = Vec.ZERO

    companion object {
        private val entityTaskMap = hashMapOf<Entity, MinestomRunnable>()
    }

    fun projectileTick(game: LazerTagGame, projectile: Entity, shooter: Player) {
        tick(game, projectile, shooter)

        if (projectile.velocity.x == 0.0 || projectile.velocity.y == 0.0 || projectile.velocity.z == 0.0 || projectile.isOnGround) return collide(
            game,
            shooter,
            projectile
        )

        val expandedBox = projectile.boundingBox.expand(boundingBoxExpand.x, boundingBoxExpand.y, boundingBoxExpand.z)
        val intersectingPlayers = game.players
            .filter { it.gameMode == GameMode.ADVENTURE && expandedBox.intersectEntity(projectile.position, it) }
            .filter { if (projectile.aliveTicks < 30) it != shooter else true }
        if (intersectingPlayers.isEmpty()) return

        collideEntity(game, shooter, projectile, intersectingPlayers)
    }

    open fun tick(game: LazerTagGame, projectile: Entity, shooter: Player) {}


    override fun shoot(game: LazerTagGame, player: Player): ConcurrentHashMap<Player, Float> {
        sound?.let { game.playSound(it, player.position) }

        if (!game.infiniteAmmo) {
            val newAmmo = (player.itemInMainHand.meta().getTag(ammoTag) ?: 1) - 1
            renderAmmo(player, newAmmo)
            player.itemInMainHand = player.itemInMainHand.withMeta {
                it.set(ammoTag, newAmmo)
            }
        }

        repeat(numberOfBullets) {
            val entity = createEntity(player).also {
                //it.setTag(playerUUIDLeastTag, player.uuid.leastSignificantBits)
                //it.setTag(playerUUIDMostTag, player.uuid.mostSignificantBits)
                it.setTag(gunIdTag, this.name)
            }

            entityTaskMap[entity] =
                object : MinestomRunnable(repeat = TaskSchedule.nextTick(), iterations = maxDuration.toLong(), taskGroup = game.taskGroup) {
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

    abstract fun projectileShot(game: LazerTagGame, player: Player): ConcurrentHashMap<Player, Float>

    fun collide(game: LazerTagGame, shooter: Player, projectile: Entity) {
        collided(game, shooter, projectile)

        entityTaskMap[projectile]?.cancel()
        projectile.remove()
    }

    fun collideEntity(game: LazerTagGame, shooter: Player, projectile: Entity, hitPlayers: Collection<Player>) {
        collidedWithEntity(game, shooter, projectile, hitPlayers)

        entityTaskMap[projectile]?.cancel()
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