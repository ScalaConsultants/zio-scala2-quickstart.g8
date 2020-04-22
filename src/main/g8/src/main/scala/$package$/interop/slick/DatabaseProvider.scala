package $package$.interop.slick

import com.typesafe.config.{Config => TypesafeConfig}
import slick.basic.BasicBackend
import slick.jdbc.H2Profile.backend._
import zio.config.Config
import zio.{UIO, ZIO, ZLayer, ZManaged}

object DatabaseProvider {

  trait Service {
    def db: UIO[BasicBackend#DatabaseDef]
  }

  val live: ZLayer[Config[TypesafeConfig], Throwable, DatabaseProvider] =
    ZLayer.fromServiceManaged { typesafeConfig =>
      ZManaged
        .make(ZIO.effect(Database.forConfig("", typesafeConfig)))(
          db => ZIO.effectTotal(db.close())
        )
        .map(
          d =>
          new DatabaseProvider.Service {
            val db: UIO[BasicBackend#DatabaseDef] = ZIO.effectTotal(d)
          }
        )
    }
}
