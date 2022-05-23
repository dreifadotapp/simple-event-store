package dreifa.app.ses

import dreifa.app.opentelemetry.Helpers
import dreifa.app.opentelemetry.OpenTelemetryProvider
import dreifa.app.opentelemetry.SpanDetails
import dreifa.app.registry.Registry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * The most simple EventStore - everything is in memory
 */
class InMemoryEventStore(registry: Registry = Registry(), initialCapacity: Int = 10) : EventStore {
    private val events: MutableList<Event> = ArrayList(initialCapacity)
    private val eventIdLookup: MutableMap<EventId, Int> = HashMap(initialCapacity)
    private val tracer = registry.getOrNull(Tracer::class.java)
    private val provider = registry.getOrNull(OpenTelemetryProvider::class.java)

    override fun read(ctx: ClientContext, query: EventQuery): List<Event> {
        return Helpers.runWithCurrentTelemetry(
            provider = provider,
            tracer = tracer,
            spanDetails = SpanDetails("events-store", SpanKind.INTERNAL),
            block = {
                val lastEventIndex = checkLastEventId(0, query)
                if (lastEventIndex == events.size) {
                    emptyList()
                } else {
                    this.events
                        .subList(lastEventIndex, events.size)
                        .filter { checkFilter(it, query) }
                }
            }
        )
    }

    override fun store(ctx: ClientContext, events: List<Event>): EventWriter {
        Helpers.runWithCurrentTelemetry(
            provider = provider,
            tracer = tracer,
            spanDetails = SpanDetails("events-store", SpanKind.INTERNAL),
            block = {
                synchronized(this) {
                    var index = this.events.size
                    events.forEach {
                        this.events.add(it)
                        eventIdLookup[it.id] = index
                        index++
                    }
                }
            }
        )
        return this
    }

    private fun checkLastEventId(lastEventIndex: Int, query: EventQuery): Int {
        return if (query is AllOfQuery) {
            var currentLastEvent = lastEventIndex
            query.forEach {
                currentLastEvent = checkLastEventId(currentLastEvent, it)
            }
            currentLastEvent
        } else if (query is LastEventId) {
            if (query.lastEventId != null) {
                val index = eventIdLookup[query.lastEventId]!!
                if (index >= lastEventIndex) index + 1 else lastEventIndex
            } else {
                lastEventIndex
            }
        } else {
            lastEventIndex
        }
    }

    override fun storeWithChecks(ctx: ClientContext, events: List<Event>) {
        TODO("Not yet implemented")
    }

    private fun checkFilter(ev: Event, query: EventQuery): Boolean {
        return when (query) {
            is AggregateIdQuery -> (query.aggregateId == ev.aggregateId)
            is EventTypeQuery -> (query.eventType == ev.type)
            is LikeEventTypeQuery -> (query.eventType.toRegex().matches(ev.type))
            is LastEventIdQuery -> true // always matches here as is processed upfront
            is EverythingQuery -> true
            is AllOfQuery -> {
                // the rule is all
                var matched = true
                query.forEach {
                    matched = matched && checkFilter(ev, it)
                }
                matched
            }
        }
    }
}