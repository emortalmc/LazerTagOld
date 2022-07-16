package dev.emortal.lazertag.raycast

import net.minestom.server.collision.BoundingBox
import net.minestom.server.entity.Entity

//private val boundingBoxToLinkedMap = HashMap<BoundingBox, LinkedBoundingBox>()
//fun BoundingBox.toLinked(entity: Entity): LinkedBoundingBox {
//    boundingBoxToLinkedMap.computeIfAbsent(this) { LinkedBoundingBox(entity, width(), height(), depth()) }
//    return boundingBoxToLinkedMap[this]!!
//}

//class LinkedBoundingBox(val entity: Entity, val width: Double, val height: Double, val depth: Double) {
//
//    val minX get() = entity.position.x() - (width / 2)
//    val maxX get() = entity.position.x() + (width / 2)
//
//    val minY get() = entity.position.y()
//    val maxY get() = entity.position.y() + height
//
//    val minZ get() = entity.position.z() - (depth / 2)
//    val maxZ get() = entity.position.z() + (depth / 2)
//
//}