package $package$.infrastructure.flyway

import com.typesafe.config.Config
import org.flywaydb.core.api.FlywayException
import zio._

trait FlywayProvider {
  def flyway: IO[FlywayException, Flyway]
}

object FlywayProvider {

  val live: RLayer[Has[Config], Has[FlywayProvider]] =
    ZLayer.fromServiceM { cfg =>
      for {
        url  <- Task(cfg.getString("url"))
        user <- Task(cfg.getString("user"))
        pwd  <- Task(cfg.getString("password"))
      } yield new FlywayProvider {
        val flyway: IO[FlywayException, Flyway] = Flyway(url, user, pwd)
      }
    }

  def flyway: ZIO[Has[FlywayProvider], FlywayException, Flyway] =
    ZIO.accessM[Has[FlywayProvider]](_.get.flyway)

}
