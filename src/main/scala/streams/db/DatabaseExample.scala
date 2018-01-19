package streams.db

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.{Done, NotUsed}

import scala.concurrent.Future

object DatabaseExample extends App {

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  val sensor = Source.tick(0.millis, 200.millis, ()).map(_ => Sample.random)

  val logger: Sink[Sample, Future[Done]] = Sink.foreach[Sample](println)

  sensor.runWith(logger).map { _ =>
    system.terminate()
  }

}
