package streams.db

import java.time.LocalDateTime

case class Sample(time: LocalDateTime, value: String)
object Sample {
  private val r = scala.util.Random
  def random = Sample(LocalDateTime.now(), r.nextDouble().toString)
}