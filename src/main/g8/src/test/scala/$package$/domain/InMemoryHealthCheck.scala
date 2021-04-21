package $package$.domain

import zio._

object InMemoryHealthCheck extends HealthCheck {

  override val healthCheck: UIO[DbStatus] = UIO.succeed(DbStatus(true))

  val test: Layer[Nothing, Has[HealthCheck]] = ZLayer.succeed(InMemoryHealthCheck)
}
