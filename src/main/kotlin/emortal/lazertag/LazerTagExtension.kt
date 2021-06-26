package emortal.lazertag

import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.block.Block
import world.cepi.kstom.raycast.HitType
import world.cepi.kstom.raycast.RayCast
import world.cepi.kstom.util.toExactBlockPosition

class LazerTagExtension : Extension() {

    override fun initialize() {
        eventNode.addListener(PlayerUseItemEvent::class.java) { e: PlayerUseItemEvent ->
            run {
                val raycast = RayCast.castRay(e.player.instance!!, e.player, e.player.position.toVector(), e.player.position.direction, 50.0)

                if (raycast.hitType == HitType.BLOCK) {
                    e.player.instance!!.setBlock(raycast.finalPosition.toExactBlockPosition(), Block.DIAMOND_BLOCK)
                }
            }
        }

        logger.info("[LazerTagExtension] has been enabled!")
    }

    override fun terminate() {
        logger.info("[LazerTagExtension] has been disabled!")
    }

}