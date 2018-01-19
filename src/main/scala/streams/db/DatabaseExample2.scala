package streams.db

import akka.Done
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

// with grouping
object DatabaseExample2 extends App {

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  val sensor: Source[Seq[Sample], Cancellable] = Source.tick(0.millis, 200.millis, ()).map(_ => Sample.random)
    .throttle(1, 1 second)
    .groupedWithin(10, 100.millis)

  def writeToDatabase(data: Seq[Sample]): Future[Int] = {
    Future {
      println(s"Wrote $data to the database")
      data.length
    }
  }

  val writeToDb = Flow[Seq[Sample]].mapAsync(10)(writeToDatabase)
  val logger: Sink[Int, Future[Done]] = Sink.foreach[Int](println)

  sensor.via(writeToDb).runWith(logger).map { _ =>
    system.terminate()
  }

}
