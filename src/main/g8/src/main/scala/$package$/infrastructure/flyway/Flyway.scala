package $package$.infrastructure.flyway

import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.{ Flyway => FlywayCore }
import zio._

final class Flyway private (private val underlying: FlywayCore) {
  def migrate: IO[FlywayException, MigrateResult] =
    IO.effect(underlying.migrate())
      .refineToOrDie[FlywayException]
      .map(MigrateResult(_))
}

object Flyway {
  def apply(url: String, user: String, password: String): IO[FlywayException, Flyway] =
    UIO
      .effectTotal(FlywayCore.configure())
      .flatMap { cfg =>
        IO.effect(cfg.dataSource(url, user, password))
          .refineToOrDie[FlywayException]
      }
      .map(_.load())
      .map(new Flyway(_))
}
