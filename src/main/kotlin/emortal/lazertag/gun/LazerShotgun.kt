package emortal.lazertag.gun

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.ItemStackBuilder
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag

class LazerShotgun : Gun("Lazer Shotgun", 2) {

    override val itemBuilder: ItemStackBuilder = ItemStack.builder(Material.WOODEN_HOE)
        .displayName(Component.text(name))
        .meta { meta: ItemMetaBuilder ->
            meta.set(Tag.Long("lastShot"), 0)
            meta.customModelData(id)
        }

    override val damage = 2f
    override val numberOfBullets = 25
    override val spread = 0.15
    override val cooldown = 500L
    override val ammo = 5
    override val maxDistance = 30.0

    override val sound = Sound.sound(SoundEvent.ZOMBIE_ATTACK_IRON_DOOR, Sound.Source.PLAYER, 1.5f, 1f)

    override fun shootAfter(player: Player) {
        player.velocity = player.position.direction.normalize().multiply(-20)
    }

}