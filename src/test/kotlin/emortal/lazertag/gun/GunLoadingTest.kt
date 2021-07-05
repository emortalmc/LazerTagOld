package emortal.lazertag.gun

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GunLoadingTest {

    @Test
    fun `ensure all guns are loaded into the map`() {
        assertEquals(4, Gun.registeredMap.size)
    }

}