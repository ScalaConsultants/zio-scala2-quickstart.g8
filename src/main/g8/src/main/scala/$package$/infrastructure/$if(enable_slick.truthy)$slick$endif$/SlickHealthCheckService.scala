package $package$.infrastructure.slick

import slick.jdbc.JdbcProfile
import slick.interop.zio.DatabaseProvider
import slick.interop.zio.syntax._

import zio._

import $package$.api.healthcheck.{ DbStatus, HealthCheckService }

class SlickHealthCheckService(databaseProvider: DatabaseProvider, jdbcProfile: JdbcProfile)
    extends HealthCheckService
    with Profile {

  override val profile = jdbcProfile

  private val healthCheckQuery = {
    import profile.api._
    sql"""select 1""".as[Int]
  }

  override def healthCheck: UIO[DbStatus] =
    ZIO
      .fromDBIO(healthCheckQuery)
      .provideLayer(ZLayer.succeed(databaseProvider))
      .fold(
        _ => DbStatus(false),
        _ => DbStatus(true)
      )
}

object SlickHealthCheckService {

  val live: RLayer[DatabaseProvider, HealthCheckService] = ZLayer {
    for {
      databaseProvider <- ZIO.service[DatabaseProvider]
      jdbcProfile      <- databaseProvider.profile
    } yield new SlickHealthCheckService(databaseProvider, jdbcProfile)
  }

}
