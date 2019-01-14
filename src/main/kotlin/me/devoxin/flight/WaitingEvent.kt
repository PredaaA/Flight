package me.devoxin.flight

import net.dv8tion.jda.core.events.Event
import java.util.concurrent.CompletableFuture

@Suppress("UNCHECKED_CAST")
class WaitingEvent<T: Event>(
        private val eventClass: Class<*>,
        private val predicate: (T) -> Boolean,
        private val future: CompletableFuture<T?>
) {

    fun check(event: Event): Boolean {
        println("checking")
        val r = eventClass.isAssignableFrom(event::class.java) && predicate(event as T)
        println(r)
        return r
    }

    fun accept(event: Event?) = future.complete(event as T)

}