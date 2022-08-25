package $package$.api.healthcheck

import zio.{ UIO, ZIO }

trait HealthCheckService {
  def healthCheck: UIO[DbStatus]
}

object HealthCheckService {

  val healthCheck: ZIO[HealthCheckService, Nothing, DbStatus] =
    ZIO.environmentWithZIO(_.get.healthCheck)

}
