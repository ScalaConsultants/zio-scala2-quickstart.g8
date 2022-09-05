package $package$.infrastructure.flyway

import com.typesafe.config.Config
import org.flywaydb.core.api.FlywayException

import zio._

trait FlywayProvider {
  def flyway: IO[FlywayException, Flyway]
}

object FlywayProvider {

  val live: RLayer[Config, FlywayProvider] = ZLayer {
    for {
      cfg  <- ZIO.service[Config]
      url  <- ZIO.attempt(cfg.getString("url"))
      user <- ZIO.attempt(cfg.getString("user"))
      pwd  <- ZIO.attempt(cfg.getString("password"))
    } yield new FlywayProvider {
      override val flyway: IO[FlywayException, Flyway] = Flyway(url, user, pwd)
    }
  }

  def flyway: ZIO[FlywayProvider, FlywayException, Flyway] =
    ZIO.environmentWithZIO[FlywayProvider](_.get.flyway)

}
