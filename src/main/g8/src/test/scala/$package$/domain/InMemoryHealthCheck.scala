package $package$.domain

import zio._

object InMemoryHealthCheck extends HealthCheck.Service {

  override val healthCheck: UIO[DbStatus] = UIO.succeed(DbStatus(true))

  val test: Layer[Nothing, HealthCheck] = ZLayer.succeed(InMemoryHealthCheck)
}
