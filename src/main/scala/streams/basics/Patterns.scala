package streams.basics

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision, ThrottleMode}

import scala.concurrent.{Future, TimeoutException}

// mapConcat is like flatMap
object Flattening1 extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  val letters: Source[Char, NotUsed] = Source('A' to 'E')
  val flattened: Source[String, NotUsed] = letters.mapConcat(letter => (1 to 3).map(index => s"$letter$index"))
  flattened.runForeach(println)
}

// use identity
object Flattening2 extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

 val future: Source[Range.Inclusive, NotUsed] = Source.fromFuture(Future.successful(1 to 10))
 val flattened: Source[Int, NotUsed] = future.mapConcat(identity)
 flattened.runForeach(println)
}

// TODO merge substreams

object Batching extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.duration._
  Source(1 to 100)
    .grouped(10)
    .runForeach(println)

  Source
  .tick(0.millis, 10.millis, ())
  .groupedWithin(100, 100.millis)
  .map { batch => println(s"Processing batch of ${batch.size} elements"); batch }
  .runWith(Sink.ignore)
}

object Async extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  def writeBatchToDatabase(batch: Seq[Int]): Future[Unit] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      println(s"Writing batch of $batch to database by ${Thread.currentThread().getName}")
    }
  }

  Source(1 to 1000000)
    .grouped(10)
    .mapAsync(10)(writeBatchToDatabase)
    .runWith(Sink.ignore)
}

object Termination extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.util.{Failure, Success}
  // success only
  Source
  .single(1)
  .runWith(Sink.ignore) // returns a Future[Done]
  .onComplete(_ => system.terminate())

  // success and failure
  Source
    .single(1)
    .watchTermination() { (_, done) =>
      done.onComplete {
        case Success(_) => println("Stream completed successfully")
        case Failure(error) => println(s"Stream failed with error ${error.getMessage}")
      }
    }
    .runWith(Sink.ignore)
}

object Throttle extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.duration._
  Source(1 to 1000000)
  .grouped(10)
  .throttle(elements = 10, per = 1.second, maximumBurst = 10, ThrottleMode.shaping)
  .mapAsync(10)((ints: Seq[Int]) => Future.successful(println(ints)))
  .runWith(Sink.ignore)
}

object Idle extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  Source
  .tick(0.millis, 1.minute, ())
  .idleTimeout(30.seconds)
  .runWith(Sink.ignore)
  .recover {
    case _: TimeoutException => println("No messages received for 30 seconds")
  }
}

object ErrorHandling extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  Source(1 to 5)
  .fold(0) { case (total, element) =>
    if (element == 3) throw new Exception("I don't like 3")
    else total + element
  }
  .withAttributes(ActorAttributes.supervisionStrategy(Supervision.restartingDecider))
  .runForeach(println)
}

object Recover extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()
  import scala.concurrent.duration._

  def pipeline =
    Source(1 to 5).map {
      case 3 => throw new Exception("three fails")
      case n => n
    }
  pipeline
    .recoverWithRetries(2, { case _ => pipeline.initialDelay(2.seconds) })
    .runForeach(println)
}

object Backoff extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()
  import scala.concurrent.duration._

  RestartSource
    .withBackoff(
      minBackoff = 2.seconds,
      maxBackoff = 30.seconds, // retries after 2, 4, 8, 16, 30 seconds
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    ) { () =>
      Source(1 to 5).map {
        case 3 => throw new Exception("connection lost!")
        case n => n
      }
    }
    .runForeach(println)

}