package emortal.lazertag

import emortal.lazertag.commands.LazerTagCommand
import emortal.lazertag.items.ItemManager
import emortal.lazertag.utils.PlayerUtils.eyePosition
import io.github.bloepiloepi.particles.shapes.ParticleShape
import io.github.bloepiloepi.particles.shapes.ShapeOptions
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.block.Block
import net.minestom.server.particle.Particle
import net.minestom.server.utils.Position
import world.cepi.kstom.Manager
import world.cepi.kstom.raycast.HitType
import world.cepi.kstom.raycast.RayCast
import world.cepi.kstom.util.toExactBlockPosition

class LazerTagExtension : Extension() {

    override fun initialize() {

        eventNode.addListener(PlayerSpawnEvent::class.java) { e: PlayerSpawnEvent -> run {
            e.player.inventory.addItemStack(ItemManager.LAZER_RIFLE.item)
        } }

        eventNode.addListener(PlayerUseItemEvent::class.java) { e: PlayerUseItemEvent ->
            run {
                val raycast = RayCast.castRay(e.player.instance!!, e.player, e.player.eyePosition().toVector(), e.player.position.direction, 50.0)

                val shapeOptions = ShapeOptions.builder(Particle.FLAME).visibleFromDistance(true).build()
                ParticleShape.line(e.player.eyePosition(), raycast.finalPosition.toPosition()).iterator(shapeOptions).draw(e.player.instance!!, Position(0.0, 0.0, 0.0))

                if (raycast.hitType == HitType.BLOCK) {
                    e.player.instance!!.setBlock(raycast.finalPosition.toExactBlockPosition(), Block.DIAMOND_BLOCK)
                }
            }
        }

        Manager.command.register(LazerTagCommand())

        logger.info("[LazerTagExtension] has been enabled!")
    }

    override fun terminate() {
        logger.info("[LazerTagExtension] has been disabled!")
    }

}