package dev.emortal.lazertag.game

import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag

object LazerTagPlayerHelper {
    val killsTag = Tag.Integer("kills")
    val deathsTag = Tag.Integer("deaths")
    val spawnProtectionUntilTag = Tag.Long("spawnProtectionUntil")

    var Player.kills: Int
        get() = getTag(killsTag) ?: 0
        set(value) = setTag(killsTag, value)

    var Player.deaths: Int
        get() = getTag(deathsTag) ?: 0
        set(value) = setTag(deathsTag, value)

    val Player.hasSpawnProtection
        get() = spawnProtectionMillis != 0L

    var Player.spawnProtectionMillis: Long?
        get() {
            val tagValue = getTag(spawnProtectionUntilTag) ?: 0
            if (tagValue == 0L || System.currentTimeMillis() > tagValue) return 0
            return tagValue - System.currentTimeMillis()
        }
        set(value) {
            if (value == null || value == 0L) {
                removeTag(spawnProtectionUntilTag)
                return
            }
            setTag(spawnProtectionUntilTag, System.currentTimeMillis() + value)
        }

    fun Player.cleanup() {
        removeTag(killsTag)
        removeTag(deathsTag)
        removeTag(spawnProtectionUntilTag)
    }
}