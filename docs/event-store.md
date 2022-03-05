# The Simple Event Store

## Defining Events

An event is simply an instance of [Event](../impl/src/main/kotlin/dreifa/app/ses/Event.kt)

At a minimum an Event should have a unique `id` and a `type`, which by convention uses Java naming conventions,
e.g. `com.example.MyEvent`. In most cases there is an `aggregateId` which links all events related to an entity such as
an orderId or a customerNumber.

Most events also have custom data, the `payload`. This can be anything that meets the following criteria.

- it is serializable by [Simple Serialisation](https://github.com/dreifadotapp/simple-serialisation#readme)
- the json serialized format fits within the current size limit, currently 32KB.

Finally, there are some common audit fields - currently just `author` and `timestamp`.

As events aren't in themselves type safe, the recommended approach is a single factory object for each event type. See
below for an example. Note that the pattern of pulling a suitable aggregateId out of the payload is quite common but
ultimately an implementation decision.

```kotlin
data class Customer(val id: String, val firstName: String, val lastName: String)

object CustomerCreatedEventFactory : EventFactory {
    fun create(customer: Customer): Event {
        return Event(
            type = "com.example.CustomerCreated",
            aggregateId = customer.id,
            payload = customer
        )
    }
}

```

## Storing Events

Storing event is very simple. 

```kotlin
val es = SimpleEventStore()

val event = SimpleEventOneFactory.create()
es.store(event)
```

## Querying Events

Use the read method with the appropriate set of filters 

```kotlin
fun `should show all query examples for docs`() {
    val es = SimpleEventStore()

    val ev1 = SimpleEventOneFactory.create(aggregateId = "order1")
    val ev2 = SimpleEventOneFactory.create(aggregateId = "order2")
    val ev3 = SimpleEventTwoFactory.create(aggregateId = "order2")
    val ev4 = SimpleEventOneFactory.create(aggregateId = "order3")
    val ev5 = FooEventFactory.create(aggregateId = "fooey")
    es.store(listOf(ev1, ev2, ev3, ev4, ev5))
    assertThat(es.read(EverythingQuery).size, equalTo(5))

    val order2 = es.read(AggregateIdQuery(aggregateId = "order2"))
    assertThat(order2, equalTo(listOf(ev2, ev3)))

    val simpleEventOne = es.read(EventTypeQuery(eventType = "SimpleEventOne"))
    assertThat(simpleEventOne, equalTo(listOf(ev1, ev2, ev4)))

    val likeSimpleEvent = es.read(LikeEventTypeQuery(eventType = LikeString("SimpleEvent%")))
    assertThat(likeSimpleEvent, equalTo(listOf(ev1, ev2, ev3, ev4)))

    val compoundQuery = es.read(
        AllOfQuery(
            listOf(
                EventTypeQuery(eventType = "SimpleEventOne"),
                AggregateIdQuery(aggregateId = "order2")
            )
        )
    )
    assertThat(compoundQuery, equalTo(listOf(ev2)))

    val afterEv3 = es.read(LastEventIdQuery(ev3.id))
    assertThat(afterEv3, equalTo(listOf(ev4, ev5)))
}

```

