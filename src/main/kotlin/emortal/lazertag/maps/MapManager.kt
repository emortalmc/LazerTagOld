package emortal.lazertag.maps

import emortal.lazertag.utils.Direction4
import emortal.lazertag.utils.VoidGenerator
import net.minestom.server.coordinate.Pos
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import world.cepi.kstom.Manager
import java.time.Duration

object MapManager {

    val mapsList = setOf("dizzymc")

    val mapMap = HashMap<String, Instance>()
    val spawnPosBlocks = HashMap<Instance, HashMap<Pos, Block>>()
    val spawnPositionMap = HashMap<String, List<Pos>>()


    fun init(extension: Extension) = mapsList.forEach {
        val mapName = it

        extension.logger.info("Loading map '$mapName'...")

        val instance = Manager.instance.createInstanceContainer()
        instance.chunkLoader = AnvilLoader(mapName)
        instance.chunkGenerator = VoidGenerator

        mapMap[mapName] = instance

        for (x in -8..8) {
            for (y in -8..8) {
                instance.loadChunk(x, y)
            }
        }

        val spawnPositionList = ArrayList<Pos>()
        val spawnPosBlocksList = HashMap<Pos, Block>()

        extension.logger.info("Checking chunks in 3 seconds!")
        Manager.scheduler.buildTask {
            extension.logger.info("Checking ${instance.chunks.size} chunks")

            for (chunk in instance.chunks) {
                for (x in 0..16) {
                    for (y in 0..256) {
                        for (z in 0..16) {
                            val xPos = (chunk.chunkX * 16) + x
                            val zPos = (chunk.chunkZ * 16) + z
                            if (chunk.getBlock(xPos, y, zPos) != Block.NETHERITE_BLOCK) continue

                            instance.setBlock(xPos, y, zPos, Block.AIR)
                            spawnPosBlocksList[Pos(xPos.toDouble(), y.toDouble(), zPos.toDouble())] = Block.NETHERITE_BLOCK

                            for (value in Direction4.values()) {
                                if (instance.getBlock(xPos + value.x, y, zPos + value.y) != Block.BEDROCK) continue

                                instance.setBlock(xPos + value.x, y, zPos + value.y, Block.AIR)
                                spawnPosBlocksList[Pos(xPos.toDouble() + value.x, y.toDouble(), zPos.toDouble() + value.y)] = Block.BEDROCK

                                spawnPositionList.add(Pos(xPos.toDouble() + 0.5, y.toDouble(), zPos.toDouble() + 0.5, value.yaw, 0f))
                            }
                        }
                    }
                }
            }

            println(spawnPosBlocksList)
            println("Loaded $mapName - found ${spawnPositionList.size} spawn locations!")

            spawnPosBlocks[instance] = spawnPosBlocksList
            spawnPositionMap[mapName] = spawnPositionList
        }.delay(Duration.ofSeconds(3)).schedule()


    }


}