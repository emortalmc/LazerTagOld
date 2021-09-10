package emortal.lazertag.game

import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag

object LazerTagPlayer {
    val isGoonTag = Tag.Byte("isGoon")

    var Player.isGoon: Boolean
        get() = this.getTag(isGoonTag)?.toInt() == 1
        set(value) = this.setTag(isGoonTag, if (value) 1 else 0)
    var Player.isSeeker: Boolean
        get() = !this.isGoon
        set(value) {
            this.isGoon = !value
        }

    fun Player.cleanup() {
        this.removeTag(isGoonTag)
        this.removeTag(isGoonTag)
    }
}