package $package$.infrastructure

import slick.interop.zio.DatabaseProvider
import slick.interop.zio.syntax._
import slick.jdbc.PostgresProfile
import $package$.api.healthcheck.{ DbStatus, HealthCheckService }
import zio._

object SlickHealthCheckService {

  val live: RLayer[DatabaseProvider, Has[HealthCheckService]] =
    ZLayer.fromFunction[DatabaseProvider, HealthCheckService] { case db =>
      new HealthCheckService with Profile {
        type P = PostgresProfile
        override lazy val profile = PostgresProfile
        import profile.api._
  
        val healthCheck: UIO[DbStatus] = {
          val query = sqlu"""select 1"""
          ZIO
            .fromDBIO(query)
            .provide(db)
            .fold(
              _ => DbStatus(false),
              _ => DbStatus(true)
            )
        }
      }
    }
}
