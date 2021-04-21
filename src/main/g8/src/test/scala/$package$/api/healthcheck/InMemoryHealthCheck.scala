package $package$.api.healthcheck

import zio._

object InMemoryHealthCheckService extends HealthCheckService {

  override val healthCheck: UIO[DbStatus] = UIO.succeed(DbStatus(true))

  val test: Layer[Nothing, Has[HealthCheckService]] = ZLayer.succeed(InMemoryHealthCheckService)
}
