package emortal.gungame.maps

import emortal.gungame.utils.VoidGenerator
import emortal.gungame.utils.Direction4
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.BlockPosition
import net.minestom.server.utils.Position
import world.cepi.kstom.Manager

object MapManager {

    val mapMap = HashMap<String, Instance>()
    val spawnPosBlocks = HashMap<Instance, HashMap<BlockPosition, Block>>()
    val spawnPositionMap = HashMap<String, List<Position>>()

    fun init() = GunGameMap.maps.forEach {
        val mapName = it.name

        println("Loading map '$mapName'...")

        val storageLocation = Manager.storage.getLocation(mapName)
        val instance = Manager.instance.createInstanceContainer(storageLocation)

        instance.chunkGenerator = VoidGenerator

        mapMap[mapName] = instance

        for (x in -8..8) {
            for (y in -8..8) {
                instance.loadChunk(x, y)
            }
        }

        val spawnPositionList = ArrayList<Position>()
        val spawnPosBlocksList = HashMap<BlockPosition, Block>()

        for (chunk in instance.chunks) {
            for (x in 0..16) {
                for (y in 0..256) {
                    for (z in 0..16) {
                        val xPos = (chunk.chunkX * 16) + x
                        val zPos = (chunk.chunkZ * 16) + z
                        if (Block.fromStateId(chunk.getBlockStateId(xPos, y, zPos)) != Block.NETHERITE_BLOCK) continue

                        instance.setBlock(xPos, y, zPos, Block.AIR)
                        spawnPosBlocksList[BlockPosition(xPos, y, zPos)] = Block.NETHERITE_BLOCK

                        for (value in Direction4.values()) {
                            if (instance.getBlock(xPos + value.x, y, zPos + value.y) != Block.BEDROCK) continue

                            instance.setBlock(xPos + value.x, y, zPos + value.y, Block.AIR)
                            spawnPosBlocksList[BlockPosition(xPos + value.x, y, zPos + value.y)] = Block.BEDROCK

                            spawnPositionList.add(Position(xPos.toDouble() + 0.5, y.toDouble(), zPos.toDouble() + 0.5, value.yaw, 0f))
                        }
                    }
                }
            }
        }

        println(spawnPosBlocksList)
        println("Loaded $mapName - found ${spawnPositionList.size} spawn locations!")

        spawnPosBlocks[instance] = spawnPosBlocksList
        spawnPositionMap[mapName] = spawnPositionList
    }


}