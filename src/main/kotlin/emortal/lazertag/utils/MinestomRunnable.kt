package emortal.lazertag.utils

import net.minestom.server.MinecraftServer
import net.minestom.server.timer.Task
import net.minestom.server.utils.time.TimeUnit

abstract class MinestomRunnable : Runnable {
    private var t: Task? = null
    private var repeatTime: Long = 0
    private var repeatUnit = TimeUnit.SECOND
    private var delayTime: Long = 0
    private var delayUnit = TimeUnit.SECOND

    fun delay(time: Long, unit: TimeUnit): MinestomRunnable {
        delayTime = time
        delayUnit = unit
        return this
    }

    fun repeat(time: Long, unit: TimeUnit): MinestomRunnable {
        repeatTime = time
        repeatUnit = unit
        return this
    }

    fun schedule(): Task {
        val t = MinecraftServer.getSchedulerManager().buildTask(this).delay(delayTime, delayUnit)
            .repeat(repeatTime, repeatUnit).schedule()
        this.t = t
        return t
    }

    fun cancel() {
        if (t == null) return
        t!!.cancel()
    }
}