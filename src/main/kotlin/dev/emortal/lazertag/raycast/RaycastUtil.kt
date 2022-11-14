package dev.emortal.lazertag.raycast

import dev.emortal.lazertag.game.LazerTagGame
import dev.emortal.lazertag.utils.breakBlock
import dev.emortal.rayfast.area.area3d.Area3d
import dev.emortal.rayfast.area.area3d.Area3dRectangularPrism
import dev.emortal.rayfast.casting.grid.GridCast
import dev.emortal.rayfast.vector.Vector3d
import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.instance.block.Block
import java.util.concurrent.ConcurrentHashMap

/**
 * Class to make Rayfast easier to use with Minestom
 *
 *
 */
object RaycastUtil {

    val boundingBoxToArea3dMap = ConcurrentHashMap<BoundingBox, Area3d>()
    private const val tolerance: Double = 0.35


    init {
        Area3d.CONVERTER.register(BoundingBox::class.java) { box ->
            boundingBoxToArea3dMap.computeIfAbsent(box) { it ->
                Area3dRectangularPrism.wrapper(
                    it,
                    { it.minX() - tolerance }, { it.minY() - tolerance }, { it.minZ() - tolerance },
                    { it.maxX() + tolerance }, { it.maxY() + tolerance }, { it.maxZ() + tolerance }
                )
            }

            boundingBoxToArea3dMap[box]
        }
    }

    val Entity.area3d: Area3d
        get() = Area3d.CONVERTER.from(boundingBox)

    @Suppress("INACCESSIBLE_TYPE")
    fun raycastBlock(game: LazerTagGame, startPoint: Point, direction: Vec, maxDistance: Double): Pos? {
        val instance = game.instance

        val gridIterator: Iterator<Vector3d> = GridCast.createExactGridIterator(
            startPoint.x(), startPoint.y(), startPoint.z(),
            direction.x(), direction.y(), direction.z(),
            1.0, maxDistance
        )

        while (gridIterator.hasNext()) {
            val gridUnit = gridIterator.next()
            val pos = Pos(gridUnit[0], gridUnit[1], gridUnit[2])

            try {
                val hitBlock = instance!!.getBlock(pos)

                if (LazerTagGame.destructableBlocks.contains(hitBlock.id())) {
                    instance.setBlock(pos, Block.AIR)
                    instance.breakBlock(pos, hitBlock)
                }

                if (LazerTagGame.collidableBlocks.contains(hitBlock.id())) {
                    return pos
                }
            } catch (e: NullPointerException) {
                // catch if chunk is not loaded
                break
            }
        }

        return null
    }

    fun raycastEntity(
        game: LazerTagGame,
        startPoint: Point,
        direction: Vec,
        maxDistance: Double,
        hitFilter: (Entity) -> Boolean = { true }
    ): Pair<Entity, Pos>? {
        
        game.instance!!.entities
            .filter { hitFilter.invoke(it) }
            .filter { it.position.distanceSquared(startPoint) <= maxDistance * maxDistance }
            .forEach {
                val area = it.area3d
                val pos = it.position

                //val intersection = it.boundingBox.boundingBoxRayIntersectionCheck(startPoint.asVec(), direction, it.position)

                val intersection = area.lineIntersection(
                    Vector3d.of(startPoint.x() - pos.x, startPoint.y() - pos.y, startPoint.z() - pos.z),
                    Vector3d.of(direction.x(), direction.y(), direction.z())
                )
                if (intersection != null) {
                    return Pair(it, Pos(intersection[0] + pos.x, intersection[1] + pos.y, intersection[2] + pos.z))
                }
            }

        return null
    }

    fun raycast(
        game: LazerTagGame,
        startPoint: Point,
        direction: Vec,
        maxDistance: Double,
        hitFilter: (Entity) -> Boolean = { true }
    ): RaycastResult {
        val blockRaycast = raycastBlock(game, startPoint, direction, maxDistance)
        val entityRaycast = raycastEntity(game, startPoint, direction, maxDistance, hitFilter)



        if (entityRaycast == null && blockRaycast == null) {
            return RaycastResult(RaycastResultType.HIT_NOTHING, null, null)
        }

        // block raycast is always true when reached
        if (entityRaycast == null) {
            return RaycastResult(RaycastResultType.HIT_BLOCK, null, blockRaycast)
        }

        // entity raycast is always true when reached
        if (blockRaycast == null) {
            return RaycastResult(RaycastResultType.HIT_ENTITY, entityRaycast.first, entityRaycast.second)
        }

        // Both entity and block check have collided, time to see which is closer!

        val distanceFromEntity = startPoint.distanceSquared(entityRaycast.second)
        val distanceFromBlock = startPoint.distanceSquared(blockRaycast)

        return if (distanceFromBlock > distanceFromEntity) {
            RaycastResult(RaycastResultType.HIT_ENTITY, entityRaycast.first, entityRaycast.second)
        } else {
            RaycastResult(RaycastResultType.HIT_BLOCK, null, blockRaycast)
        }

    }

}
