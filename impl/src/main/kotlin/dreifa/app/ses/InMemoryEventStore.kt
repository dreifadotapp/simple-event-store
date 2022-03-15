package dreifa.app.ses

import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * The most simple EventStore - everything is in memory
 */
class InMemoryEventStore(initialCapacity: Int = 10) : EventStore {
    private val events: MutableList<Event> = ArrayList(initialCapacity)
    private val eventIdLookup: MutableMap<EventId, Int> = HashMap(initialCapacity)
    override fun read(query: EventQuery): List<Event> {
        val lastEventIndex = checkLastEventId(0, query)
        if (lastEventIndex == events.size) return emptyList()
        return this.events
            .subList(lastEventIndex, events.size)
            .filter { checkFilter(it, query) }
    }

    override fun store(events: List<Event>): EventWriter {
        synchronized(this) {
            var index = this.events.size
            events.forEach {
                this.events.add(it)
                eventIdLookup[it.id] = index
                index++
            }
        }
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


    override fun storeWithChecks(events: List<Event>) {
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