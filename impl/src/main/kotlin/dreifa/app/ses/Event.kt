package dreifa.app.ses

import java.util.*


/**
 * Don't fix the event Id to a specific type just in case UUID
 * doesn't guarantee enough uniqueness with large streams
 */
class EventId(private val id: UUID = UUID.randomUUID()) {
    override fun toString(): String = id.toString()

    companion object {
        // only use a string returned to toString()
        fun fromString(id: String): EventId {
            return EventId(UUID.fromString(id))
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other is EventId) {
            other.id == this.id
        } else false
    }

    override fun hashCode(): Int = this.id.hashCode()

}

/**
 * Data class to define an Event.
 */
data class Event(
    /**
     * Every event must have an unique Id
     */
    val id: EventId = EventId(),

    /**
     * The event type. This can be any string value, however
     * the java naming convention is recommended, e.g. 'com.example.MyEvent'
     */
    val type: String,

    /**
     * Most events are linked a domain model of some type. By convention this
     * is referenced by an 'aggregateId'. Typical examples are an orderId or
     * customerNumber
     */
    val aggregateId: String? = null,

    /**
     * Most event also have some data. This can be anything that is manageable
     * by the simple-serialisation framework and or a reasonable size
     * (currently defined as no more 32KB when serialised to JSON)
     */
    val payload: Any? = null,

    /**
     * An optional creator, mainly for auditing and history
     * Limited to 255 characters
     */
    val creator: String? = null,

    /**
     * The timestamp in the unix timestamp format.
     */
    val timestamp: Long = System.currentTimeMillis(),

    )

// marker interface (is this useful)
interface EventFactory {
    fun eventType(): String
    fun typeFilter(): EventQuery = EventTypeQuery(eventType())
}

object OppsEventFactory : EventFactory {
    fun get(): Event {
        return Event(type = "OppsEvent")
    }

    override fun eventType(): String = "OppsEvent"
}