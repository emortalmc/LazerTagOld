package dev.emortal.lazertag.gun

import dev.emortal.immortal.util.progressBar
import dev.emortal.lazertag.game.LazerTagGame
import dev.emortal.lazertag.raycast.RaycastResultType
import dev.emortal.lazertag.raycast.RaycastUtil
import dev.emortal.lazertag.utils.breakBlock
import dev.emortal.lazertag.utils.sendBlockDamage
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import world.cepi.kstom.item.and
import world.cepi.kstom.item.item
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

sealed class Gun(val name: String, private val customMeta: (ItemMetaBuilder) -> Unit = {}) {

    companion object {
        val gunIdTag = Tag.String("gunID")
        val taskIdTag = Tag.Integer("taskID")
        val lastShotTag = Tag.Long("lastShot")
        val reloadingTag = Tag.Byte("reloading")
        val ammoTag = Tag.Integer("ammo")

        val registeredMap: Map<String, Gun>
            get() = Gun::class.sealedSubclasses.mapNotNull { it.objectInstance }.associateBy { it.name } +
                    ProjectileGun::class.sealedSubclasses.mapNotNull { it.objectInstance }.associateBy { it.name }

        var Player.heldGun: Gun?
            get() = registeredMap[itemInMainHand.getTag(gunIdTag)]
            set(value) {
                this.itemInMainHand = value?.item ?: ItemStack.AIR
            }
    }

    abstract val material: Material
    abstract val color: TextColor

    val item by lazy {
        item(material) {
            displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false))
            set(gunIdTag, name)
            set(lastShotTag, 0)
            set(ammoTag, ammo)
            hideFlag()
            customMeta.invoke(this)
        }
    }

    abstract val damage: Float // PER BULLET!
    open val numberOfBullets: Int = 1
    open val spread: Double = 0.0
    abstract val cooldown: Int // In ticks
    abstract val ammo: Int
    abstract val reloadTime: Int // In ticks
    open val freshReload: Boolean = true
    open val shootMidReload: Boolean = false
    open val maxDistance: Double = 10.0

    open val burstAmount: Int = 1
    open val burstInterval: Int = 0 // In ticks

    open val sound: Sound? = Sound.sound(SoundEvent.ENTITY_BLAZE_HURT, Sound.Source.PLAYER, 1f, 1f)

    fun getReloadTicks(currentAmmo: Int): Int {
        return if (freshReload) {
            reloadTime
        } else {
            (reloadTime.toFloat() * ((ammo.toFloat() - currentAmmo.toFloat()) / ammo.toFloat())).toInt()
        }
    }

    open fun shoot(game: LazerTagGame, player: Player): HashMap<Player, Float> {
        sound?.let { game.playSound(it, player.position) }

        val newAmmo = (player.itemInMainHand.meta.getTag(ammoTag) ?: 1) - 1
        renderAmmo(player, newAmmo)
        player.itemInMainHand = player.itemInMainHand.and {
            setTag(ammoTag, newAmmo)
        }

        return gunShot(game, player)
    }

    protected open fun gunShot(game: LazerTagGame, player: Player): HashMap<Player, Float> {
        val damageMap = HashMap<Player, Float>()

        val instance = player.instance!!
        val eyePos = player.eyePosition()
        val eyeDir = player.position.direction()

        repeat(numberOfBullets) {

            val direction = eyeDir.spread(spread)//.normalize()

            val raycastResult = RaycastUtil.raycast(instance, eyePos, direction, maxDistance) {
                it != player && it.entityType == EntityType.PLAYER && (it as Player).gameMode == GameMode.ADVENTURE
            }

            instance.showParticle(
                Particle.particle(
                    type = ParticleType.DUST_COLOR_TRANSITION,
                    count = 1,
                    data = OffsetAndSpeed(),
                    extraData = DustTransition(1f, 1f, 0f, 0f, 0f, 0f, 0.7f),
                    longDistance = true
                ),
                Vectors(
                    eyePos.asVec(),
                    (raycastResult.hitPosition ?: eyePos.add(direction.mul(maxDistance))).asVec(),
                    1.5
                )
            )

            when (raycastResult.resultType) {
                RaycastResultType.HIT_ENTITY -> { // Hit entity
                    val hitPlayer: Player = raycastResult.hitEntity!! as Player

                    val headshot = (hitPlayer.position.y + 1.25) < raycastResult.hitPosition!!.y()

                    if (headshot) {
                        player.playSound(
                            Sound.sound(SoundEvent.BLOCK_GLASS_BREAK, Sound.Source.BLOCK, 0.5f, 1.2f)
                        )
                    }

                    damageMap[hitPlayer] = damageMap.getOrDefault(hitPlayer, 0f) + (damage * (if (headshot) 2f else 1f))
                }
                RaycastResultType.HIT_BLOCK -> { // Hit block
                    instance.playSound(
                        Sound.sound(SoundEvent.BLOCK_NETHERRACK_BREAK, Sound.Source.BLOCK, 2f, 1f),
                        raycastResult.hitPosition!!
                    )
                    val blockType = instance.getBlock(raycastResult.hitPosition)
                    instance.breakBlock(raycastResult.hitPosition, blockType)
                    instance.sendBlockDamage(1, raycastResult.hitPosition)
                }
                else -> { // Hit nothing

                }
            }

        }

        shootAfter(player)

        return damageMap
    }

    protected open fun shootAfter(player: Player) {}

    open fun renderAmmo(
        player: Player,
        currentAmmo: Int,
        percentage: Float = currentAmmo.toFloat() / ammo.toFloat(),
        reloading: Boolean = false
    ) {
        val component = Component.text()

        if (reloading) component.append(Component.text("RELOADING ", NamedTextColor.RED, TextDecoration.BOLD))

        component.append(
            progressBar(
                percentage,
                40,
                "|",
                if (reloading) NamedTextColor.RED else NamedTextColor.GOLD,
                NamedTextColor.DARK_GRAY
            )
        )

        component.append(
            Component.text(
                " ${String.format("%0${ammo.toString().length}d", currentAmmo)}/$ammo",
                NamedTextColor.DARK_GRAY
            )
        )

        player.sendActionBar(component)
    }
}