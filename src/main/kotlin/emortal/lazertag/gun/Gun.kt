package emortal.lazertag.gun

import emortal.immortal.particle.ParticleUtils
import emortal.immortal.particle.shapes.sendParticle
import emortal.lazertag.raycast.RaycastResultType
import emortal.lazertag.raycast.RaycastUtil
import emortal.lazertag.utils.MathUtils
import emortal.lazertag.utils.sendBlockDamage
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import world.cepi.kstom.util.*
import kotlin.collections.set

sealed class Gun(val name: String, val customMeta: (ItemMetaBuilder) -> Unit = {}) {

    companion object {
        val gunIdTag = Tag.String("gunID")
        val playerUUIDTag = Tag.String("playerUUID")
        val lastShotTag = Tag.Long("lastShot")
        val reloadingTag = Tag.Byte("reloading")
        val ammoTag = Tag.Integer("ammo")

        val registeredMap: Map<String, Gun>
            get() = Gun::class.sealedSubclasses.mapNotNull { it.objectInstance }.associateBy { it.name }

        var Player.heldGun: Gun?
            get() = registeredMap.get(this.itemInMainHand.getTag(gunIdTag))
            set(value) {
                this.itemInMainHand = value?.item ?: ItemStack.AIR
            }
    }

    open val material: Material = Material.WOODEN_HOE
    open val color: TextColor = NamedTextColor.WHITE

    val item by lazy {
        ItemStack.builder(material)
            .displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false))
            .meta { meta: ItemMetaBuilder ->
                meta.set(gunIdTag, name)
                meta.set(lastShotTag, 0)
                meta.set(reloadingTag, 0)
                meta.set(ammoTag, ammo)
                customMeta.invoke(meta)
                meta
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

            val raycastResult = RaycastUtil.raycast(instance, eyePos, direction, maxDistance) {
                it != player && it.entityType == EntityType.PLAYER && (it as Player).gameMode == GameMode.ADVENTURE
            }

            when (raycastResult.resultType) {
                RaycastResultType.HIT_ENTITY -> { // Hit entity
                    val (x, y, z) = raycastResult.hitPosition!!

                    val hitPlayer: Player = raycastResult.hitEntity!! as Player

                    damageMap[hitPlayer] = damageMap.getOrDefault(hitPlayer, 0f) + damage


                    val particleShape = ParticleUtils.line(
                        ParticleUtils.colored(
                            Particle.DUST_COLOR_TRANSITION,
                            0.0, 0.0, 0.0, 0f, 0f, 0f,
                            NamedTextColor.YELLOW,
                            NamedTextColor.YELLOW,
                            0.2f,
                            1,
                            0f
                        ),
                        Vec(eyePos.x(), eyePos.y(), eyePos.z()),
                        Vec(x, y, z),
                        1.5
                    )
                }
                RaycastResultType.HIT_BLOCK -> { // Hit block
                    val (x, y, z) = raycastResult.hitPosition!!

                    val particleShape = ParticleUtils.line(
                        ParticleUtils.colored(
                            Particle.DUST_COLOR_TRANSITION,
                            0.0, 0.0, 0.0, 0f, 0f, 0f,
                            NamedTextColor.YELLOW,
                            NamedTextColor.YELLOW,
                            0.2f,
                            1,
                            0f
                        ),
                        Vec(eyePos.x(), eyePos.y(), eyePos.z()),
                        Vec(x, y, z),
                        1.5
                    )

                    instance.sendParticle(particleShape)


                    instance.sendParticle(ParticleUtils.particle(Particle.LARGE_SMOKE, x, y, z, count = 1))
                    instance.playSound(Sound.sound(SoundEvent.BLOCK_NETHERRACK_BREAK, Sound.Source.BLOCK, 2f, 1f), x, y, z)
                    instance.sendBlockDamage(1, Pos(x, y, z))
                }
                else -> { // Hit nothing

                }
            }

        }

        val newAmmo = player.itemInMainHand.meta.getTag(ammoTag)!! - 1
        renderAmmo(player, newAmmo)
        player.itemInMainHand = player.itemInMainHand.withMeta { meta: ItemMetaBuilder ->
            meta.set(ammoTag, newAmmo)
        }

        shootAfter(player)

        return damageMap
    }

    open fun shootAfter(player: Player) {

    }

    open fun collide(player: Player, projectile: Entity) {

    }

    open fun renderAmmo(player: Player, currentAmmo: Int) {
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