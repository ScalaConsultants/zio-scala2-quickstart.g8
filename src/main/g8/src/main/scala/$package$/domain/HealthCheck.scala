package $package$.domain

import zio.UIO

trait HealthCheck {

  val healthCheck: UIO[DbStatus]

}
