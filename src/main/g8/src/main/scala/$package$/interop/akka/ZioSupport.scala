package $package$.interop

import akka.http.scaladsl.marshalling.{ Marshaller, Marshalling }
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{ Route, RouteResult }
import zio.{ DefaultRuntime, Task, ZIO }

import scala.concurrent.{ Future, Promise }
import scala.language.implicitConversions

trait ZioSupport extends DefaultRuntime { self =>

  implicit def zioMarshaller[A](implicit m1: Marshaller[A, HttpResponse], m2: Marshaller[Throwable, HttpResponse]): Marshaller[Task[A], HttpResponse] =
    Marshaller { implicit ec => a => {
      val r = a.foldM(
        e => Task.fromFuture(implicit ec => m2(e)), 
        a => Task.fromFuture(implicit ec => m1(a))
      )
      
      val p = Promise[List[Marshalling[HttpResponse]]]()
      
      self.unsafeRunAsync(r) { exit => 
        exit.fold(e => p.failure(e.squash), s => p.success(s))
      }
      
      p.future
    }}

  private def fromFunction[A, B](f: A => Future[B]): ZIO[A, Throwable, B] = for {
    a  <- ZIO.fromFunction(f)
    b  <- ZIO.fromFuture(_ => a)
  } yield b

  implicit def zioRoute(z: ZIO[Any, Throwable, Route]): Route = ctx => {
    val p = Promise[RouteResult]()

    val f = z.flatMap(r => fromFunction(r)).provide(ctx)

    self.unsafeRunAsync(f) { exit => 
      exit.fold(e => p.failure(e.squash), s => p.success(s))
    }

    p.future
  }
  
}
