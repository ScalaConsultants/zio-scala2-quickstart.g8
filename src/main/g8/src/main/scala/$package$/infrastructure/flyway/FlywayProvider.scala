package $package$.infrastructure.flyway

import com.typesafe.config.Config
import org.flywaydb.core.api.FlywayException
import zio._

object FlywayProvider {

  trait Service {
    def flyway: IO[FlywayException, Flyway]
  }

  object Service {
    def apply(url: String, user: String, password: String): FlywayProvider.Service =
      new FlywayProvider.Service {
        override val flyway: IO[FlywayException, Flyway] = Flyway(url, user, password)
      }
  }

  val live: RLayer[Has[Config], FlywayProvider] =
    ZLayer.fromServiceM { cfg =>
      for {
        url  <- Task(cfg.getString("url"))
        user <- Task(cfg.getString("user"))
        pwd  <- Task(cfg.getString("password"))
      } yield FlywayProvider.Service(url, user, pwd)
    }

  def flyway: ZIO[FlywayProvider, FlywayException, Flyway] =
    ZIO.accessM[FlywayProvider](_.get.flyway)

}
