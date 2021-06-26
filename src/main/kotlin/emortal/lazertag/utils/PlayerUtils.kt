package emortal.lazertag.utils

import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.BlockChangePacket
import net.minestom.server.particle.Particle
import net.minestom.server.particle.ParticleCreator
import net.minestom.server.utils.BlockPosition
import net.minestom.server.utils.Position

object PlayerUtils {
    /**
     * Sends a block to a specific player (Only client-side/visual change)
     * @param player Player to send block to
     * @param blockPos Position of the block
     * @param blockType Material of the block
     */
    fun sendBlockChange(player: Player, blockPos: BlockPosition?, blockType: Block) {
        val packet = BlockChangePacket()
        packet.blockStateId = blockType.blockId.toInt()
        packet.blockPosition = blockPos
        player.playerConnection.sendPacket(packet)
    }

    /**
     * Sends a particle to a specific player
     * @param player Player to send particle to
     * @param particle The type of the particle
     * @param pos The position of the particle
     * @param offsetX The X randomness
     * @param offsetY The Y randomness
     * @param offsetZ The Z randomness
     * @param count The amount of particles to send
     */
    fun sendParticle(
        player: Player,
        particle: Particle?,
        pos: Position,
        offsetX: Float,
        offsetY: Float,
        offsetZ: Float,
        count: Int
    ) {
        player.playerConnection.sendPacket(
            ParticleCreator.createParticlePacket(
                particle, false,
                pos.x, pos.y, pos.z,
                offsetX, offsetY, offsetZ, 0f, count, null
            )
        )
    }
}