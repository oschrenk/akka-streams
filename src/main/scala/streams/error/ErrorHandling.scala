package streams.error

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorAttributes._
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import akka.stream.scaladsl.Source

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ErrorHandling extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()
  import scala.concurrent.ExecutionContext.Implicits.global

  Source(1 to 5)
    .fold(0) { case (total, element) =>
      if (element == 3) throw new Exception("I don't like 3")
      else total + element
    }
    .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
    .runForeach(println)
    .onComplete {
      case Failure(_) =>
        println("Fail")
        system.terminate()
      case Success(_) =>
        println("Done.")
        system.terminate()
    }
}
