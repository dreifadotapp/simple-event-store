package dreifa.app.ses

import dreifa.app.sis.JsonSerialiser
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 *
 */
class FileEventStore(private val rootDirectory: String = ".") : EventStore {
    private val simpleEventStore = InMemoryEventStore()
    private val serialiser = JsonSerialiser()
    private var eventCount = 0

    init {
        File(rootDirectory).mkdirs()

        Files.list(Paths.get(rootDirectory)).sorted().forEach {
            println(it)
            val file = it.toFile()
            if (file.isFile) {
                val ev = jsonToEvent(file.readText())
                simpleEventStore.store(ev)
            }
        }


    }

    override fun read(query: EventQuery): List<Event> {
        return simpleEventStore.read(query)
    }

    override fun store(events: List<Event>): EventWriter {
        synchronized(this) {
            events.forEach {
                eventCount++
                val fileName =
                    eventCount.toString().padStart(5, '0') + "-event.json"

                File("${rootDirectory}/$fileName").writeText(eventToJson(it))
                simpleEventStore.store(it)
            }
        }
        return this
    }

    override fun storeWithChecks(events: List<Event>) {
        TODO("Not yet implemented")
    }

    private fun eventToJson(ev: Event): String {
        val payload = if (ev.payload != null) serialiser.serialiseData(ev.payload) else null
        val serializeable = SerializableEvent(
            id = ev.id.toString(),
            type = ev.type,
            aggregateId = ev.aggregateId,
            payloadAsJson = payload,
            creator = ev.creator,
            timestamp = ev.timestamp
        )
        return serialiser.serialiseData(serializeable)
    }

    private fun jsonToEvent(json: String): Event {
        val serializeable = serialiser.deserialiseData(json).any() as SerializableEvent
        val payload =
            if (serializeable.payloadAsJson != null) {
                serialiser.deserialiseData(serializeable.payloadAsJson).any()
            } else null

        return Event(
            id = EventId.fromString(serializeable.id),
            type = serializeable.type,
            aggregateId = serializeable.aggregateId,
            payload = payload,
            creator = serializeable.creator,
            timestamp = serializeable.timestamp
        )

    }


}

data class SerializableEvent(
    val id: String,
    val type: String,
    val aggregateId: String?,
    val payloadAsJson: String?,
    val creator: String?,
    val timestamp: Long
)

