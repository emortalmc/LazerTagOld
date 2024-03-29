package dev.emortal.lazertag.gun

import dev.emortal.lazertag.entity.NoDragEntityProjectile
import dev.emortal.lazertag.game.LazerTagGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ExplosionPacket
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import java.time.Duration

object HomingMissile : ProjectileGun("Homing Missile", Rarity.IMPOSSIBLE) {

    private val accuracyTag = Tag.Double("accuracy")

    override val material: Material = Material.NETHER_STAR
    override val color: TextColor = NamedTextColor.GOLD

    override val damage = 999f
    override val ammo = 10
    override val reloadTime: Int = 0
    override val cooldown: Int = 50

    override val sound = null

    override fun projectileShot(game: LazerTagGame, player: Player): Map<Player, Float> {
        return emptyMap()
    }

    override fun collided(game: LazerTagGame, shooter: Player, projectile: Entity) {
        shooter.instance!!.sendGroupedPacket(ExplosionPacket(projectile.position.x(), projectile.position.y(), projectile.position.z(), 3f, ByteArray(0), 0f, 0f, 0f))

        shooter.instance!!.playSound(
            Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.PLAYER, 3f, 1f),
            projectile.position
        )

        shooter.instance!!.players
            .filter { it.gameMode == GameMode.ADVENTURE }
            .filter { it.getDistanceSquared(projectile) < 2.5 * 2.5 }
            .forEach { loopedPlayer ->
                loopedPlayer.velocity =
                    loopedPlayer.position.sub(projectile.position.sub(.0, .5, .0)).asVec().normalize().mul(60.0)

                game.damage(
                    shooter,
                    loopedPlayer,
                    false,
                    if (loopedPlayer == shooter) 2f else damage
                )
            }

        projectile.remove()
    }

    override fun tick(game: LazerTagGame, projectile: Entity, shooter: Player) {
        val closestPlayer = game.players.filter { it != shooter && it.gameMode == GameMode.ADVENTURE }.minByOrNull { it.getDistanceSquared(projectile) }
        if (closestPlayer == null) {
            projectile.setNoGravity(false)
            projectile.velocity = shooter.position.direction().mul(50.0)
            return
        }

        val dirToClosestPlayer = closestPlayer.position.add(0.0, 1.0, 0.0).sub(projectile.position).asVec().normalize()

        val distance = projectile.getDistance(closestPlayer)

        if (distance < 4)
            projectile.setTag(accuracyTag, projectile.getTag(accuracyTag) + (4 + 1) - distance / 8)

        if (distance < 2.2) collide(game, shooter, projectile)

        projectile.velocity = projectile.velocity.add(dirToClosestPlayer.mul(projectile.getTag(accuracyTag) * 30.0)).normalize().mul(30.0)
    }

    override fun createEntity(shooter: Player): Entity {
        val projectile = NoDragEntityProjectile(shooter, EntityType.PIG)

        projectile.setTag(accuracyTag, 0.15)
        projectile.setNoGravity(true)
        projectile.scheduleRemove(Duration.ofSeconds(10))
        projectile.setInstance(shooter.instance!!, shooter.position.add(0.0, shooter.eyeHeight, 0.0))

        return projectile
    }

}