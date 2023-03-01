package dev.emortal.lazertag.config

import dev.emortal.immortal.serializer.PositionSerializer
import kotlinx.serialization.Serializable
import net.minestom.server.coordinate.Pos

@Serializable
class LazerTagConfig(
    var spawnPositions: MutableMap<String, Array<@Serializable(with = PositionSerializer::class) Pos>> = mutableMapOf()

)