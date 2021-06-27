package emortal.lazertag.utils

import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.BlockChangePacket
import net.minestom.server.utils.BlockPosition
import net.minestom.server.utils.Position

object PlayerUtils {
    /**
     * Gets a player's position + their eye height added to their Y position
     */
    fun Entity.eyePosition(): Position {
        if (this.isSneaking) return position.clone().add(0.0, 1.23, 0.0)
        return position.clone().add(0.0, 1.53, 0.0)
    }

    fun Player.playSound(sound: Sound, position: Position) {
        playSound(sound, position.x, position.y, position.z)
    }

    fun Instance.playSound(sound: Sound, position: Position) {
        playSound(sound, position.x, position.y, position.z)
    }

    /**
     * Sends a block to a specific player (Only client-side/visual change)
     * @param blockPos Position of the block
     * @param blockType Material of the block
     */
    fun Player.sendBlockChange(blockPos: BlockPosition, blockType: Block) {
        val packet = BlockChangePacket()
        packet.blockStateId = blockType.blockId.toInt()
        packet.blockPosition = blockPos
        playerConnection.sendPacket(packet)
    }


}