package $package$.application

import zio.{ Has, ZIO }
import $package$.domain._

object HealthCheckService {

  val healthCheck: ZIO[Has[HealthCheck], Nothing, DbStatus] =
    ZIO.accessM(_.get.healthCheck)

}
