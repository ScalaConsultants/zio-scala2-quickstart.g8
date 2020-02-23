package $package$.interop.slick

import zio.{ UIO, ZIO }
import zio.interop.reactiveStreams._
import zio.stream.ZStream
import slick.dbio.{ DBIO, StreamingDBIO }
import slick.basic.BasicBackend

trait DatabaseProvider {
  def databaseProvider: DatabaseProvider.Service
}

object DatabaseProvider {
  trait Service {
    def db: UIO[BasicBackend#DatabaseDef]
  }
}

object dbio {

  implicit class ZIOObjOps(private val obj: ZIO.type) extends AnyVal {
    def fromDBIO[R](dbio: DBIO[R]): ZIO[DatabaseProvider, Throwable, R] =
      for {
        db <- ZIO.accessM[DatabaseProvider](_.databaseProvider.db)
        r  <- ZIO.fromFuture(ec => db.run(dbio))
      } yield r

    def fromStreamingDBIO[T](dbio: StreamingDBIO[_, T]): ZIO[DatabaseProvider, Throwable, ZStream[Any, Throwable, T]] =
      for {
        db <- ZIO.accessM[DatabaseProvider](_.databaseProvider.db)
        r  <- ZIO.effect(db.stream(dbio).toStream())
      } yield r
  }

}

