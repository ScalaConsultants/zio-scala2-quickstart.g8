package $package$.infrastructure

import slick.interop.zio.DatabaseProvider
import slick.interop.zio.syntax._
import slick.jdbc.PostgresProfile
import $package$.domain.{ DbStatus, HealthCheck, ItemId }
import $package$.infrastructure.tables.ItemsTable
import zio.logging._
import zio._

final class SlickHealthCheck(env: DatabaseProvider with Logging)
    extends HealthCheck
    with ItemsTable
    with Profile {
  type P = PostgresProfile
  override lazy val profile = PostgresProfile
  import profile.api._

  val items = table

  val healthCheck: UIO[DbStatus] = {
    val query = items.filter(_.id === ItemId(-1)).result
    ZIO
      .fromDBIO(query)
      .provide(env)
      .fold(
        _ => DbStatus(false),
        _ => DbStatus(true)
      )
  }

}

object SlickHealthCheck {

  val live: RLayer[DatabaseProvider with Logging, Has[HealthCheck]] =
    ZLayer.fromFunctionM(env => ZIO.succeed(new SlickHealthCheck(env)))
}
