package emortal.lazertag.items

import emortal.lazertag.gun.BeeCannon
import emortal.lazertag.gun.LazerMinigun
import emortal.lazertag.gun.LazerShotgun
import emortal.lazertag.gun.Rifle
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

object ItemManager {
    val LAZER_MINIGUN = LazerMinigun()
    val LAZER_SHOTGUN = LazerShotgun()
    val RIFLE = Rifle()
    val ROCKET_LAUNCHER = BeeCannon()

    val KNIFE = ItemStack.builder(Material.IRON_SWORD).displayName(Component.text("Knife", NamedTextColor.GRAY)).build()
}