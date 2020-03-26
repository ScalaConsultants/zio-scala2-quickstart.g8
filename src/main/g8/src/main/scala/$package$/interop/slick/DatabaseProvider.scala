package $package$.interop.slick

import slick.basic.BasicBackend
import slick.jdbc.H2Profile.backend._
import zio.config.Config
import zio.{ UIO, ZIO, ZLayer, ZManaged }

object DatabaseProvider {

  trait Service {
    def db: UIO[BasicBackend#DatabaseDef]
  }

  val live: ZLayer[Config[DatabaseConfig], Throwable, DatabaseProvider] =
    ZLayer.fromServiceManaged { c: Config.Service[DatabaseConfig] =>
      ZManaged
        .make(ZIO.effect(Database.forConfig("", c.config.underlying)))(db => ZIO.effectTotal(db.close()))
        .map(d =>
          new DatabaseProvider.Service {
            val db: UIO[BasicBackend#DatabaseDef] = ZIO.effectTotal(d)
          }
        )
    }
}
