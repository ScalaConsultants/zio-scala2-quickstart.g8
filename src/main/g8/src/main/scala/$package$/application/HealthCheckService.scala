package $package$.application

import zio.ZIO
import $package$.domain._

object HealthCheckService {

  val healthCheck: ZIO[HealthCheck, Nothing, DbStatus] =
    ZIO.accessM(_.get.healthCheck)

}
