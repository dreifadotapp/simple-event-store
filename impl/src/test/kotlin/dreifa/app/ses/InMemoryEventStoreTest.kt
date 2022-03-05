package dreifa.app.ses

class InMemoryEventStoreTest : BaseEventStoreTest() {
    override fun createEventStore(): EventStore = InMemoryEventStore()
}