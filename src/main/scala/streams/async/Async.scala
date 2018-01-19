package streams.async

import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object Async extends App {

  def compress(s: String): Array[Byte] = {
    val baos = new ByteArrayOutputStream(s.length)
    val gzip = new GZIPOutputStream(baos)
    gzip.write(s.getBytes("UTF-8"))
    gzip.close()
    val bytes = baos.toByteArray
    baos.close()
    bytes
  }

  def uncompress(b: Array[Byte]): String = {
    val gzip = new GZIPInputStream(new ByteArrayInputStream(b))
    val br = new BufferedReader(new InputStreamReader(gzip, "UTF-8"))
    val sb = new StringBuilder()
    var line = ""
    var cont = true
    while (cont) {
      line = br.readLine()
      if (line == null) {
        cont =false
      } else {
        sb.append(line)
      }
    }
    br.close()
    gzip.close()
    sb.toString()
  }

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()
  import scala.concurrent.ExecutionContext.Implicits.global

  val start = System.currentTimeMillis()
  Source(1 to 200000)
    .map(_.toString)
    .map(compress)
    .async
    .map(uncompress)
    .runWith(Sink.ignore)
    .map{_ =>
      val end = System.currentTimeMillis()
      println(s"finished in ${end -start} millis")
      system.terminate()
    }


}
