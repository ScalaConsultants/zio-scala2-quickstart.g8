package $package$.domain

import zio.{UIO}

object HealthCheck {

  trait Service {

    val healthCheck: UIO[DbStatus]

  }
}
