package streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

object Finite {
  val empty: Source[Nothing, NotUsed] = Source.empty
  val single: Source[String, NotUsed] = Source.single("single element")
  val range: Source[Int, NotUsed] = Source(1 to 3)
}

object Infinite {
  val repeat: Source[Int, NotUsed] = Source.repeat(5)
}

object Runner extends App {
  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  Finite.empty.runForeach(println)
  Finite.single.runForeach(println)
  Finite.range.runForeach(println)

  Infinite.repeat.take(5).runForeach(println)

  system.terminate()
}
