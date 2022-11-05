package $package$.infrastructure.quill

import io.getquill._
import io.getquill.jdbczio.Quill

import zio._

import $package$.api.healthcheck.{ DbStatus, HealthCheckService }

class QuillHealthCheckService(quill: Quill.Postgres[Literal]) extends HealthCheckService {
  import quill._

  override def healthCheck: UIO[DbStatus] =
    run(healthCheckQuery)
      .fold(
        _ => DbStatus(false),
        _ => DbStatus(true)
      )

  private val healthCheckQuery = quote {
    sql"""SELECT 1""".as[Query[Int]]
  }
}

object QuillHealthCheckService {

  val live: RLayer[Quill.Postgres[Literal], QuillHealthCheckService] =
    ZLayer.fromFunction(new QuillHealthCheckService(_))

}
