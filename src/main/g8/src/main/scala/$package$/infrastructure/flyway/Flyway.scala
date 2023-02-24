package $package$.infrastructure.flyway

import javax.sql.DataSource

import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.{ Flyway => FlywayCore }

import zio._

final class Flyway private (private val underlying: FlywayCore) {
  def migrate: IO[FlywayException, MigrateResult] =
    ZIO
      .attempt(underlying.migrate())
      .refineToOrDie[FlywayException]
      .map(MigrateResult(_))
}

object Flyway {
  def apply(ds: DataSource): IO[FlywayException, Flyway] =
    ZIO
      .attempt(FlywayCore.configure().dataSource(ds).load)
      .refineToOrDie[FlywayException]
      .map(flywayCore => new Flyway(flywayCore))

}
