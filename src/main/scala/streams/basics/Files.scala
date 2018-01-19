package streams.basics

import java.io.File
import java.nio.file.Path

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Flow, Framing, Keep, Sink, Source}
import akka.util.ByteString

object Files extends App {

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val lines: Flow[ByteString, String, NotUsed] = Framing.delimiter(ByteString(System.lineSeparator), 10000, allowTruncation = true).map(bs => bs.utf8String)
  val lineCounter = Flow[Path].flatMapConcat(path => FileIO.fromPath(path).via(lines)).fold(0l)((count, line) => count + 1).toMat(Sink.head)(Keep.right)
  val testFiles = Source(List("somePathToFile1", "somePathToFile2").map(new File(_).toPath))
  testFiles runWith lineCounter foreach println
}
