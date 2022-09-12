package $package$.api.healthcheck

import zio._

object InMemoryHealthCheckService extends HealthCheckService {

  override val healthCheck: UIO[DbStatus] = ZIO.succeed(DbStatus(true))

  val test: Layer[Nothing, HealthCheckService] = ZLayer.succeed(InMemoryHealthCheckService)
}
