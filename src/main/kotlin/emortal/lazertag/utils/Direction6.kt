package emortal.lazertag.utils

enum class Direction6(val x: Int, val y: Int, val z: Int) {
    NORTH(0, 0, -1),
    EAST(1, 0, 0),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1 ,0)
}