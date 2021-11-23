package emortal.lazertag.gun

import emortal.lazertag.raycast.RaycastResultType
import emortal.lazertag.raycast.RaycastUtil
import emortal.lazertag.utils.sendBlockDamage
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import world.cepi.kstom.util.asVec
import world.cepi.kstom.util.eyePosition
import world.cepi.kstom.util.playSound
import world.cepi.kstom.util.spread
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.extra.DustTransition
import world.cepi.particle.showParticle
import world.cepi.particle.util.Vectors
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

            instance.showParticle(
                Particle.particle(
                    type = ParticleType.DUST_COLOR_TRANSITION,
                    count = 1,
                    data = OffsetAndSpeed(0f, 0f, 0f, 0f),
                    extraData = DustTransition(1f, 1f, 0f, 1f, 1f, 1f, scale = 1f)
                ),
                Vectors(
                    eyePos.asVec(),
                    (raycastResult.hitPosition ?: eyePos.add(eyePos.asVec().normalize().mul(maxDistance))).asVec(),
                    1.5
                )
            )

            when (raycastResult.resultType) {
                RaycastResultType.HIT_ENTITY -> { // Hit entity
                    val hitPlayer: Player = raycastResult.hitEntity!! as Player

                    damageMap[hitPlayer] = damageMap.getOrDefault(hitPlayer, 0f) + damage
                }
                RaycastResultType.HIT_BLOCK -> { // Hit block
                    instance.playSound(
                        Sound.sound(SoundEvent.BLOCK_NETHERRACK_BREAK, Sound.Source.BLOCK, 2f, 1f),
                        raycastResult.hitPosition!!
                    )
                    instance.sendBlockDamage(1, raycastResult.hitPosition)
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
                .append(
                    Component.text(
                        " ${String.format("%0${ammo.toString().length}d", currentAmmo)}/$ammo",
                        NamedTextColor.DARK_GRAY
                    )
                )
                .build()
        )
    }
}