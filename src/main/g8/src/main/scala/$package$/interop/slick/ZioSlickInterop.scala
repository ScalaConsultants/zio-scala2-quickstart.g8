package $package$.interop.slick

import _root_.slick.basic.BasicBackend
import _root_.slick.dbio.{ DBIO, StreamingDBIO }
import _root_.slick.jdbc.H2Profile.backend._
import zio.interop.reactiveStreams._
import zio.stream.ZStream
import zio.{ Layer, UIO, ZIO, ZLayer, ZManaged }

object DatabaseProvider {

  trait Service {
    def db: UIO[BasicBackend#DatabaseDef]
  }

  val live: Layer[Throwable, DatabaseProvider] =
    ZLayer.fromManaged(
      ZManaged
        .make(ZIO.effect(Database.forURL("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")))(db =>
          ZIO.effectTotal(db.close())
        )
        .map(d =>
          new DatabaseProvider.Service {
            val db: UIO[BasicBackend#DatabaseDef] = ZIO.effectTotal(d)
          }
        )
    )
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
