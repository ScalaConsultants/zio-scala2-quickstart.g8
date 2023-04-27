package $package$.infrastructure.flyway

import javax.sql.DataSource

import org.flywaydb.core.api.FlywayException

import zio._

trait FlywayProvider {
  def flyway: IO[FlywayException, Flyway]
}

object FlywayProvider {

  val live: RLayer[DataSource, FlywayProvider] = ZLayer {
    for {
      ds <- ZIO.service[DataSource]
    } yield new FlywayProvider {
      override val flyway: IO[FlywayException, Flyway] = Flyway(ds)
    }
  }

  def flyway: ZIO[FlywayProvider, FlywayException, Flyway] =
    ZIO.environmentWithZIO[FlywayProvider](_.get.flyway)

}
