# Simple Event Store (ses)

[![Circle CI](https://circleci.com/gh/dreifadotapp/simple-event-store.svg?style=shield)](https://circleci.com/gh/dreifadotapp/simple-event-store)
[![Licence Status](https://img.shields.io/github/license/dreifadotapp/simple-event-store)](https://github.com/dreifadotapp/simple-event-store/blob/master/licence.txt)

## What it does

Simple Event Store is just a minimalist implementation of an event store for the
[Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html) pattern.

Deployed to [jitpack](https://jitpack.io). See [releases](https://github.com/dreifadotapp/simple-event-store/releases) for
version details. To include in your project, if using gradle:

```groovy 

\\ add jitpack repo 
maven { url "https://jitpack.io" }

\\ include the dependency 
implementation 'com.github.dreifa:simple-event-store:<version>'
```

_JitPack build status is at https://jitpack.io/com/github/dreifadotapp/simple-event-store/$releaseTag/build.log_

Two implementations are provided:

* `InMemoryEventStore` works in memory and is only intended for use within unit tests and examples.
* `FileEventStore` persists to file system. It is not intended for production usage.

The anticipation is that other implementations will be provided for production, for example a `JpaEventStore` that is
backed by a relational database. To support this the units will be refactored into a suite of common tests that should
pass for any event store.

There are two basic restrictions on an individual event:

* the event `payload` (i.e. tha actual data) must conform to rules
  of [Simple Serialisation(sis)](https://github.com/dreifadotapp/simple-serialisation#readme).
* there is a limit of on size of an event, current set to 32K for the payload in its serialised json format. This is for
  query efficiency; the event must be small enough to fit with size limit of single row for database - there is no
  industry agreed standard here, but in practice all the main stream database allow at least 64K in their current
  releases so 32KB feels a reasonable limit, allowing plenty of room for other columns in the row.

## Dependencies

As with everything in [Dreifa dot App](https://dreifadotapp.app), this library has minimal dependencies.

* Kotlin 1.4
* Java 11
* The object [Registry](https://github.com/dreifadotapp/registry#readme)
* The [Simple Serialisation(sis)](https://github.com/dreifadotapp/simple-serialisation#readme) module
    - [Jackson](https://github.com/FasterXML/jackson) for JSON serialisation

## Next Steps

* [Using](docs/event-store.md) the event store.
* [Managing Consistency](docs/event-consistency.md) when writing events.