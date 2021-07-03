package emortal.lazertag.gun

import emortal.lazertag.utils.ParticleUtils
import emortal.lazertag.utils.PlayerUtils.eyePosition
import emortal.lazertag.utils.RandomUtils
import emortal.lazertag.utils.RandomUtils.spread
import io.github.bloepiloepi.particles.shapes.ParticleShape
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.color.Color
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.ItemStackBuilder
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.utils.Position
import net.minestom.server.utils.Vector
import world.cepi.kstom.raycast.HitType
import world.cepi.kstom.raycast.RayCast

sealed class Gun(val name: String, val id: Int) {

    companion object {
        val registeredMap = HashMap<Int, Gun>()
    }

    init {
        if (registeredMap.containsKey(id)) {
            println("Duplicate gun IDs")
        }
        registeredMap[id] = this
    }

    open val itemBuilder: ItemStackBuilder = ItemStack.builder(Material.WOODEN_HOE)
        .displayName(Component.text(name))
        .meta { meta: ItemMetaBuilder ->
            meta.set(Tag.Long("lastShot"), 0)
            meta.customModelData(id)
        }

    val item by lazy { itemBuilder.build() }
    val maxDurability by lazy { item.meta.damage }

    open val damage: Float = 1f // PER BULLET!
    open val numberOfBullets: Int = 1
    open val spread: Double = 0.0
    open val cooldown: Long = 1 // In millis
    open val ammo: Int = 10
    open val maxDistance: Double = 10.0

    open val burstAmount: Int = 1
    open val burstInterval: Long = 0 // In ticks

    open val sound: Sound = Sound.sound(SoundEvent.BLAZE_HURT, Sound.Source.PLAYER, 1f, 1f)

    open fun shoot(player: Player): HashMap<Player, Float> {
        val damageMap = HashMap<Player, Float>()

        val instance = player.instance!!
        val eyePos = player.eyePosition()
        val eyeDir = eyePos.direction

        repeat(numberOfBullets) {

            val direction = eyeDir.spread(spread).normalize()

            val raycast = RayCast.castRay(
                instance,
                player,
                eyePos.toVector(),
                direction,
                maxDistance,
                0.5,
                acceptEntity = { _: Vector, entity: Entity ->
                    entity.entityType == EntityType.PLAYER && (entity as Player).gameMode == GameMode.ADVENTURE
                }, // Accept if player is in adventure mode (Prevents spectators blocking bullets)
                margin = 0.3
            )
            val lastPos = raycast.finalPosition.toPosition()

            if (raycast.hitType == HitType.ENTITY) {
                val hitPlayer: Player = raycast.hitEntity!! as Player

                val shapeOptions = ParticleUtils.getColouredShapeOptions(Color(255, 0, 0), Color(20, 20, 20), 1.5f)
                ParticleShape.line(raycast.finalPosition.subtract(direction.multiply(6)).toPosition(), lastPos)
                    .iterator(shapeOptions).draw(instance, RandomUtils.ZERO_POS)

                damageMap[hitPlayer] = damageMap.getOrDefault(hitPlayer, 0f) + damage
            } else {
                val shapeOptions = ParticleUtils.getColouredShapeOptions(Color(100, 100, 100), Color(50, 50, 50), 0.2f)
                ParticleShape.line(eyePos, lastPos)
                    .iterator(shapeOptions).draw(instance, Position(0.0, 0.0, 0.0))
            }

        }

        shootAfter(player)

        return damageMap
    }

    open fun shootAfter(player: Player) {

    }
}