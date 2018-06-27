package streams.error

import akka.Done
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

// idle
// watch
// recover
// recoverWithRetry
object MatAndRecovery extends App {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()
  import scala.concurrent.duration._
  val (cancel, done) = Source.tick(0.millis, 1000.millis, ())
    .map(_ => scala.util.Random.nextInt(6).toString)
    // somebody tell me a number between 0 and 5
    .map(e => if (e == "3") throw new IllegalArgumentException("Bang") else e)
//    .recover{case _: Exception => "Dodge"}
    .toMat(Sink.foreach(println))(Keep.both).run()

  val a = akka.pattern.after(duration = 10.seconds, using = system.scheduler)(Future.successful(cancel.cancel()))
  val f = Future.firstCompletedOf(Seq(done, a))

  f.onComplete{
    case Failure(e) =>
      println(s"Stream failed with $e")
      system.terminate()
    case Success(v) =>
      println(s"Stream finished with $v")
      system.terminate()
  }



}
