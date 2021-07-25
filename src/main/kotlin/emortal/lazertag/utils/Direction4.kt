package emortal.lazertag.utils

enum class Direction4(val x: Int, val y: Int, val yaw: Float) {
    NORTH(0, -1, 180f),
    EAST(1, 0, -90f),
    SOUTH(0, 1, 0f),
    WEST(-1, 0, 90f)
}