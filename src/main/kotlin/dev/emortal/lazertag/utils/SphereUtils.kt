package dev.emortal.lazertag.utils

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec

fun getPointsInSphere(point: Point, radius: Int): List<Point> {
    val list = mutableListOf<Point>()

    for (x in -radius..radius) {
        for (y in -radius..radius) {
            for (z in -radius..radius) {
                if (x * x + y * y + z * z < radius * radius) {
                    list.add(Vec(point.x() + x.toDouble(), point.y() + y.toDouble(), point.z() + z.toDouble()))
                }
            }
        }
    }

    return list
}