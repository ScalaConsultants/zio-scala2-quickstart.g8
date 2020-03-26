package $package$.interop.slick

import slick.dbio.{ DBIO, StreamingDBIO }
import zio.ZIO
import zio.interop.reactivestreams._
import zio.stream.ZStream

object syntax {

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
