package streams.basics

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}

import scala.collection.immutable
import scala.concurrent.Future

object SimpleSources {
  object Finite {
    val empty: Source[Nothing, NotUsed] = Source.empty
    val single: Source[String, NotUsed] = Source.single("single element")
    val range: Source[Int, NotUsed] = Source(1 to 3)
    val future: Source[String, NotUsed] = Source.fromFuture(Future.successful("Future!"))
  }

  object Infinite {
    val repeat: Source[Int, NotUsed] = Source.repeat(5)
  }
}

object SourcesRunner extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  private val d1: Future[Done] = SimpleSources.Finite.empty.runForeach(println)
  private val d2: Future[Done] = SimpleSources.Finite.single.runForeach(println)
  private val d3: Future[Done] = SimpleSources.Finite.range.runForeach(println)
  private val d4: Future[Done] = SimpleSources.Finite.future.runForeach(println)

  private val d5: Future[Done] = SimpleSources.Infinite.repeat.take(5).runForeach(println)

  import scala.concurrent.ExecutionContext.Implicits.global
  private val allDone: Future[Seq[Done]] = Future.sequence(Seq(d1,d2,d3,d4,d5))
  allDone.onComplete(_ => system.terminate())
}

object SimpleFlows {
  val stringify: Flow[Int, String, NotUsed] = Flow[Int].map(i => i.toString)
  val double: Flow[Int, Int, NotUsed] = Flow[Int].map(_ * 2)
  val selectEven: Flow[Int, Int, NotUsed] = Flow[Int].filter(_ % 2 == 0)
  val batch: Flow[Int, immutable.Seq[Int], NotUsed] = Flow[Int].grouped(10)
}


object FlowsRunner extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  private val d1: Future[Done] = SimpleSources.Finite.range.via(SimpleFlows.double).runForeach(println)
  private val d2: Future[Done] = SimpleSources.Finite.range.via(SimpleFlows.selectEven).runForeach(println)
  private val d3: Future[Done] = SimpleSources.Finite.range.via(SimpleFlows.batch).runForeach(i => println(s"batch $i"))

  import scala.concurrent.ExecutionContext.Implicits.global
  private val allDone: Future[Seq[Done]] = Future.sequence(Seq(d1, d2, d3))
  allDone.onComplete(_ => system.terminate())
}

object SimpleSinks {
  val print: Sink[String, Future[Done]] = Sink.foreach[String](println(_))
  val sum: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
  val head: Sink[Nothing, Future[Nothing]] = Sink.head
  val noop: Sink[Any, Future[Done]] = Sink.ignore
}

object SinksRunner extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  private val graph: RunnableGraph[Future[Done]] =
    // what is to Mat?
    // what is to vs toMat?
    // what does Keep.right do?
    SimpleSources.Finite.range.via(SimpleFlows.stringify).toMat(SimpleSinks.print)(Keep.right)
  val d1: Future[Done] = graph.run()

  import scala.concurrent.ExecutionContext.Implicits.global
  private val allDone: Future[Seq[Done]] = Future.sequence(Seq(d1))
  allDone.onComplete(_ => system.terminate())
}
