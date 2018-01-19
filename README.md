# Akka Streams

Reactive Streams are lazy and asynchronous by default.
This means one explicitly has to request the evaluation of the stream. In Akka Streams this can be done through the run* methods.

```
val repeat: Source[Int, NotUsed] = Source.repeat(5)
```

What is the second type for a `Source`?

> The Source type is parameterized with two types: the first one is the type of element that this source emits and the second one may signal that running the source produces some auxiliary value (e.g. a network source may provide information about the bound port or the peer’s address). Where no auxiliary information is produced, the type akka.NotUsed is used—and a simple range of integers surely falls into this category.
