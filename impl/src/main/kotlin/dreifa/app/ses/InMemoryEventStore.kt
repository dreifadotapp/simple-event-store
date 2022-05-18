package dreifa.app.ses

import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.opentelemetry.OpenTelemetryProvider
import dreifa.app.registry.Registry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
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
        val trc = TelemetryRequestContext(ctx, "events-read")
        return runWithTelemetry(trc) {

            val lastEventIndex = checkLastEventId(0, query)
            if (lastEventIndex == events.size) {
                emptyList()
            } else {
                this.events
                    .subList(lastEventIndex, events.size)
                    .filter { checkFilter(it, query) }
            }
        }
    }

    override fun store(ctx: ClientContext, events: List<Event>): EventWriter {
        val trc = TelemetryRequestContext(ctx, "events-store")
        runWithTelemetry(trc) {
            synchronized(this) {
                var index = this.events.size
                events.forEach {
                    this.events.add(it)
                    eventIdLookup[it.id] = index
                    index++
                }
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

    data class TelemetryRequestContext(val ctx: ClientContext, val spanName: String)

    data class TelemetryExecutionContext(val otc: OpenTelemetryContext)


    fun <O> runWithTelemetry(
        trc: TelemetryRequestContext, block: ((tec: TelemetryExecutionContext) -> O)
    ): O {
        return if (provider != null && tracer != null) {
            val span = tracer.spanBuilder(trc.spanName).setSpanKind(SpanKind.SERVER).startSpan()

            try {
                val telemetryContext = OpenTelemetryContext.fromSpan(span)
                val result = block.invoke(
                    TelemetryExecutionContext(telemetryContext),
                )  // what if the result is streaming ? are we closing the span too soon?
                completeSpan(span)
                result
            } catch (ex: Exception) {
                completeSpan(span, ex)
                throw ex
            }
        } else {
            block.invoke(TelemetryExecutionContext(OpenTelemetryContext.root()))
        }
    }


    private fun completeSpan(span: Span) {
        span.setStatus(StatusCode.OK)
        span.end()
    }

    private fun completeSpan(span: Span, ex: Throwable) {
        span.recordException(ex)
        span.setStatus(StatusCode.ERROR, ex.message!!)
        span.end()
    }


}