package emortal.lazertag.gun

import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerUseItemEvent

class a {
    init {
        MinecraftServer.getGlobalEventHandler().addListener(
            PlayerUseItemEvent::class.java
        ) { e: PlayerUseItemEvent -> e.player }
    }
}