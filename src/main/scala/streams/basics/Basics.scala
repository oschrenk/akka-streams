package streams.basics

import akka.actor.{ActorRef, ActorSystem, Cancellable}
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.{Done, NotUsed}

import scala.concurrent.Future

object Sources extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()
  import scala.concurrent.duration._

  // lots of convenience methods
  val empty: Source[Nothing, NotUsed] = Source.empty
  val single: Source[String, NotUsed] = Source.single("single element")
  val range: Source[Int, NotUsed] = Source(1 to 3)
  val actor: Source[Int, ActorRef] =
    Source.actorRef[Int](bufferSize = 0, overflowStrategy = OverflowStrategy.fail)
  val future: Source[String, NotUsed] = Source.fromFuture(Future.successful("Hello"))
  val repeat: Source[Int, NotUsed] = Source.repeat(5)
  val tick: Source[String, Cancellable] = Source.tick(0.second, 1.second, "Tick")

  val eventualDone: Future[Done] = range.runForeach(println)

  //TODO add Source.fromFuture
    // TODO Source.tick
}

object Sinks extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  val foreach: Sink[Int, Future[Done]] = Sink.foreach[Int](println)
  val ignore: Sink[Any, Future[Done]] = Sink.ignore
  val head = Sink.head
  val fold = Sink.fold[Int, Int](0)((acc, e) => acc + e)

  val source: Source[Int, NotUsed] = Source(1 to 3)
  val flow: RunnableGraph[NotUsed] = source.to(fold)
  flow.run()
}

object Flows extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 3)
  val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)
  val invert: Flow[Int, Int, NotUsed] = Flow[Int].map(elem => elem * -1)
  val doubler: Flow[Int, Int, NotUsed] = Flow[Int].map(elem => elem * 2)
  val runnable: RunnableGraph[NotUsed] = source via invert via doubler to sink
  runnable.run()
}
object AsyncBoundaries extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  import akka.stream.Attributes.asyncBoundary

  val runnable: RunnableGraph[NotUsed] = Source(List(1, 2, 3))
    .map(_ + 1).async
    .withAttributes(asyncBoundary)
    .map(_ * 2)
    .to(Sink.ignore)
  runnable.run()

  val invert: Flow[Int, Int, NotUsed] = Flow[Int].map(elem => elem * -1)
  val doubler: Flow[Int, Int, NotUsed] = Flow[Int].map(elem => elem * 2)

  Source(1 to 1000000)
    .via(invert).async
    .via(doubler).async
    .runWith(Sink.ignore)
}
