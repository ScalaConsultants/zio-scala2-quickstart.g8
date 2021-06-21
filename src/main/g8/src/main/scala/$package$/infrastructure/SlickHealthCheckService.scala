package $package$.infrastructure

import slick.interop.zio.DatabaseProvider
import slick.interop.zio.syntax._
import $package$.api.healthcheck.{ DbStatus, HealthCheckService }
import zio._

object SlickHealthCheckService {

  val live: RLayer[Has[DatabaseProvider], Has[HealthCheckService]] =
    ZLayer.fromServiceM { db =>
      db.profile.map { jdbcProfile =>
        new HealthCheckService with Profile {
          override lazy val profile = jdbcProfile
          import profile.api._

          val healthCheck: UIO[DbStatus] = {
            val query = sql"""select 1""".as[Int]
            ZIO
              .fromDBIO(query)
              .provide(Has(db))
              .fold(
                _ => DbStatus(false),
                _ => DbStatus(true)
              )
          }
        }
      }
    }
}
