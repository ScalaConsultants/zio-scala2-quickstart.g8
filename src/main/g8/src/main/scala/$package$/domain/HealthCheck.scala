package $package$.domain

import zio.{IO, UIO}

object HealthCheck {

  trait Service {

    val healthCheck: UIO[DbStatus]

  }
}
