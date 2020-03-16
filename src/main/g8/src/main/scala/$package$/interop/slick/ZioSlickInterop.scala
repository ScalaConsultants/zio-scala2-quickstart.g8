package $package$.interop.slick

import slick.basic.BasicBackend
import slick.dbio.{ DBIO, StreamingDBIO }
import slick.jdbc.H2Profile.backend._
import zio.config.Config
import zio.interop.reactiveStreams._
import zio.stream.ZStream
import zio.{ UIO, ZIO, ZLayer, ZManaged }
import $package$.api.Api.DbConfig

object DatabaseProvider {

  trait Service {
    def db: UIO[BasicBackend#DatabaseDef]
  }

  val live: ZLayer[Config[DbConfig], Throwable, DatabaseProvider] =
    ZLayer.fromServiceManaged { c: Config.Service[DbConfig] =>
      ZManaged
        .make(ZIO.effect(Database.forURL(c.config.url, driver = c.config.driver)))(db =>
          ZIO.effectTotal(db.close())
        )
        .map(d =>
          new DatabaseProvider.Service {
            val db: UIO[BasicBackend#DatabaseDef] = ZIO.effectTotal(d)
          }
        )
    }

}

object dbio {

  implicit class ZIOObjOps(private val obj: ZIO.type) extends AnyVal {
    def fromDBIO[R](dbio: DBIO[R]): ZIO[DatabaseProvider, Throwable, R] =
      for {
        db <- ZIO.accessM[DatabaseProvider](_.get.db)
        r  <- ZIO.fromFuture(_ => db.run(dbio))
      } yield r

    def fromStreamingDBIO[T](dbio: StreamingDBIO[_, T]): ZIO[DatabaseProvider, Throwable, ZStream[Any, Throwable, T]] =
      for {
        db <- ZIO.accessM[DatabaseProvider](_.get.db)
        r  <- ZIO.effect(db.stream(dbio).toStream())
      } yield r
  }

}
