package streams.db

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

// with async
object DatabaseExample1 extends App {

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  val sensor = Source.tick(0.millis, 20.millis, ()).map(_ => Sample.random)
    .throttle(1, 1 second)

    def writeToDatabase(data: Sample): Future[Int] = {
    Future {
      println(s"Wrote $data to the database")
      1
    }
  }

  val writeToDb = Flow[Sample].mapAsync(10)(writeToDatabase)

  val logger: Sink[Sample, Future[Done]] = Sink.foreach[Sample](println)

  sensor.runWith(logger).map { _ =>
    system.terminate()
  }

}
