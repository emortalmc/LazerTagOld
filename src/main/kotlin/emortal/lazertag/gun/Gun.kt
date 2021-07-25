package emortal.lazertag.gun

import emortal.immortal.particle.ParticleUtils
import emortal.immortal.particle.shapes.sendParticle
import emortal.lazertag.utils.MathUtils
import emortal.lazertag.utils.sendBlockDamage
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import world.cepi.kstom.raycast.HitType
import world.cepi.kstom.raycast.RayCast
import world.cepi.kstom.util.eyePosition
import world.cepi.kstom.util.playSound
import world.cepi.kstom.util.spread

sealed class Gun(val name: String, val id: Int) {

    companion object {
        val registeredMap: Map<Int, Gun>
            get() = Gun::class.sealedSubclasses.map { it.objectInstance }.filterNotNull().associateBy { it.id }
    }

    open val material: Material = Material.WOODEN_HOE
    open val color: TextColor = NamedTextColor.WHITE

    val item by lazy {
        ItemStack.builder(material)
            .displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false))
            .meta { meta: ItemMetaBuilder ->
                meta.set(Tag.Long("lastShot"), 0)
                meta.set(Tag.Byte("reloading"), 0)
                meta.set(Tag.Integer("ammo"), ammo)
                meta.customModelData(id)
            }.build()
    }

    open val damage: Float = 1f // PER BULLET!
    open val numberOfBullets: Int = 1
    open val spread: Double = 0.0
    open val cooldown: Long = 1L // In millis
    open val ammo: Int = 10
    open val reloadTime: Long = 2000L // In millis
    open val maxDistance: Double = 10.0

    open val burstAmount: Int = 1
    open val burstInterval: Long = 0 // In ticks

    open val sound: Sound = Sound.sound(SoundEvent.ENTITY_BLAZE_HURT, Sound.Source.PLAYER, 1f, 1f)

    open fun shoot(player: Player): HashMap<Player, Float> {
        val damageMap = HashMap<Player, Float>()

        val instance = player.instance!!
        val eyePos = player.eyePosition()
        val eyeDir = player.position.direction()

        repeat(numberOfBullets) {

            val direction = eyeDir.spread(spread).normalize()

            val raycast = RayCast.castRay(
                instance,
                player,
                eyePos,
                direction,
                maxDistance,
                0.5,
                acceptEntity = { _: Point, entity: Entity ->
                    entity is Player && entity.gameMode == GameMode.ADVENTURE /*&& entity.team != player.team*/
                }, // Accept if entity is a player and is in adventure mode (prevents spectators blocking bullets) and is not on the same team
                margin = 0.3
            )

            if (raycast.hitType == HitType.ENTITY) {
                val hitPlayer: Player = raycast.hitEntity!! as Player

                damageMap[hitPlayer] = damageMap.getOrDefault(hitPlayer, 0f) + damage
            } else if (raycast.hitType == HitType.BLOCK) {
                instance.sendParticle(ParticleUtils.particle(Particle.LARGE_SMOKE, raycast.finalPosition, Vec(0.25, 0.25, 0.25),1, 0f))
                //instance.sendParticle(ParticleUtils.vibration(eyePos, raycast.finalPosition, 20))

                instance.playSound(Sound.sound(SoundEvent.BLOCK_NETHERRACK_BREAK, Sound.Source.BLOCK, 2f, 1f), raycast.finalPosition)
                instance.sendBlockDamage(1, raycast.finalPosition)
            }

        }

        shootAfter(player)

        return damageMap
    }

    open fun shootAfter(player: Player) {

    }

    open fun collide(player: Player, projectile: Entity) {

    }

    fun renderAmmo(player: Player, currentAmmo: Int) {
        val blocks = 40
        val ammoPercentage: Float = currentAmmo.toFloat() / ammo.toFloat()
        val completedBlocks: Int = (ammoPercentage * blocks).toInt()
        val incompleteBlocks: Int = blocks - completedBlocks

        player.sendActionBar(
            Component.text()
                .append(Component.text("|".repeat(completedBlocks), NamedTextColor.GOLD))
                .append(Component.text("|".repeat(incompleteBlocks), NamedTextColor.DARK_GRAY))
                .append(Component.text(" ${String.format("%0${MathUtils.digitsInNumber(ammo)}d", currentAmmo)}/$ammo", NamedTextColor.DARK_GRAY))
                .build()
        )
    }
}