package emortal.lazertag.utils

import net.minestom.server.utils.Position
import net.minestom.server.utils.Vector
import java.util.concurrent.ThreadLocalRandom

object RandomUtils {

    val ZERO_POS = Position(0.0, 0.0, 0.0);

    fun Vector.spread(spread: Double): Vector {
        val vec = this.clone()
        if (spread == 0.0) return vec;
        val threadLocalRandom = ThreadLocalRandom.current();

        vec.rotateAroundX(threadLocalRandom.nextDouble(-spread, spread))
        vec.rotateAroundY(threadLocalRandom.nextDouble(-spread, spread))

        return vec
    }

}