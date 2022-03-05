package dreifa.app.ses

object SimpleEventOneFactory : EventFactory {
    fun create(aggregateId: String = "order1"): Event = Event(type = eventType(), aggregateId = aggregateId)
    override fun eventType(): String = "SimpleEventOne"
}

object SimpleEventTwoFactory : EventFactory {
    fun create(aggregateId: String = "order1"): Event = Event(type = eventType(), aggregateId = aggregateId)
    override fun eventType(): String = "SimpleEventTwo"
}

data class Foo(val value: String = "foobar")
object FooEventFactory : EventFactory {
    fun create(aggregateId: String = "order1", payload: Foo = Foo()): Event =
        Event(type = eventType(), aggregateId = aggregateId, payload = payload)

    override fun eventType(): String = "FooEvent"
}


