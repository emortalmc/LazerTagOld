package emortal.lazertag.utils

import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity

class BetterBoundingBox(val entity: Entity, val topLeft: Point, val bottomRight: Point) : BoundingBox(entity, 0.0, 0.0, 0.0) {

    override fun getMaxX(): Double = topLeft.x()
    override fun getMaxY(): Double = topLeft.y()
    override fun getMaxZ(): Double = topLeft.z()

    override fun getMinX(): Double = bottomRight.x()
    override fun getMinY(): Double = bottomRight.y()
    override fun getMinZ(): Double = bottomRight.z()

}