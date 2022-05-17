package dreifa.app.ses

import dreifa.app.clock.PlatformTimer
import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.types.LikeString
import java.lang.RuntimeException

data class ClientContext(val telemetryContext: OpenTelemetryContext) {
    companion object {
        fun noop(): ClientContext = ClientContext(OpenTelemetryContext.root())
    }
}

interface EventWriter {
    fun store(ctx: ClientContext, events: List<Event>): EventWriter

    fun store(events: List<Event>): EventWriter = store(ClientContext.noop(), events)

    fun storeWithChecks(ctx: ClientContext, events: List<Event>)

    fun storeWithChecks(events: List<Event>) = storeWithChecks(ClientContext.noop(), events)

    fun store(ctx: ClientContext, event: Event): EventWriter = store(ctx, listOf(event))

    fun store(event: Event): EventWriter = store(ClientContext.noop(), event)

}


interface EventReader {
    fun read(ctx: ClientContext, query: EventQuery): List<Event>

    fun read(query: EventQuery): List<Event> = read(ClientContext.noop(), query)


    /**
     * A simple polling that will block until at least
     * one event matching the query is found.
     *
     * Note that as this this implementation actively polls it
     * should be used with care in production like applications
     */
    fun pollForEvent(
        ctx: ClientContext,
        query: EventQuery,
        delayInTicks: Int = 5,
        timeoutMs: Long = 10000
    ) {
        val cutOff = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < cutOff) {
            if (this.read(ctx, query).isNotEmpty()) return
            PlatformTimer.sleepForTicks(delayInTicks)
        }
        throw ESException("Timed out waiting for event")
    }

    fun pollForEvent(
        query: EventQuery,
        delayInTicks: Int = 5,
        timeoutMs: Long = 10000
    ) = pollForEvent(dreifa.app.ses.ClientContext.Companion.noop(), query, delayInTicks, timeoutMs)
}

interface EventStore : EventReader, EventWriter

// build up a list of specific exceptions
sealed class ESExceptions(message: String) : RuntimeException(message)
class ESLockingException(message: String) : ESExceptions(message)
class ESException(message: String) : ESExceptions(message)


/**
 * In many cases consumers have already consumed events, and simply
 * just want any new ones
 */
interface LastEventId {
    val lastEventId: EventId?
}

// all the possible queries
sealed class EventQuery

/**
 * Common queries
 */
data class AggregateIdQuery(val aggregateId: String) : EventQuery()
data class EventTypeQuery(val eventType: String) : EventQuery()
data class LikeEventTypeQuery(val eventType: LikeString) : EventQuery()
data class LastEventIdQuery(override val lastEventId: EventId) : LastEventId, EventQuery()

object EverythingQuery : EventQuery()


// All queries must match
class AllOfQuery(private val queries: List<EventQuery>) : Iterable<EventQuery>, EventQuery() {
    constructor(vararg queries: EventQuery) : this(queries.asList())

    override fun iterator(): Iterator<EventQuery> = queries.listIterator()
}
