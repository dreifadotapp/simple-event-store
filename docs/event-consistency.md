# Event Consistency

An event store typically doesn't provide any locking or transactions services, other than guaranteeing that a list is
committed in its entirety or not at all. Instead, consistency is normally managed in the application layer. This works
well with the DDD concept of a [Root Aggregate](https://martinfowler.com/bliki/DDD_Aggregate.html). For example all
updates to an order should go through an "Order" root aggregate which can check the current state. In this example,
provided the application can ensure that each order has its own instance of the order object and importantly that it is
a singleton (_i.e. only one instance per order_) the updates to an individual order are single threaded and there is no
need for any form of locking. Ensuring only a single (active) instance per order is an annoying but managable problem for
monolith application (its essentially the Actor model). It becomes harder in a micro services world, and is harder again
in a truly distributed system.

So really we need to build some type of distributed locking into the design.

To be continued... (spoiler alert - I think not as hard as it might sound)

