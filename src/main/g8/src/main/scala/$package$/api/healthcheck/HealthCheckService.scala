package $package$.api.healthcheck

import zio.{ Has, UIO, ZIO }

trait HealthCheckService {
  def healthCheck: UIO[DbStatus]
}

object HealthCheckService {

  val healthCheck: ZIO[Has[HealthCheckService], Nothing, DbStatus] =
    ZIO.accessM(_.get.healthCheck)

}
