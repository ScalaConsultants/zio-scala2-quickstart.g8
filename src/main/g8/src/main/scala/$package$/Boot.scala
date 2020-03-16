package $package$

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import $package$.api.Api
import $package$.api.Api.ApiConfig
import $package$.infrastructure._
import $package$.interop.slick.DatabaseProvider
import zio.console._
import zio.{ App, ZIO, ZLayer }

import scala.concurrent.ExecutionContext

object Boot extends App {

  val host = "0.0.0.0"
  val port = 8080

  val program: ZIO[Console with Api, Throwable, Unit] = ZIO.effect {

    // todo: cleaner to make it a ZManaged module
    implicit val system: ActorSystem = ActorSystem("zio-example-system")

    val ec: ExecutionContext = system.dispatcher

    for {
      binding <- ZIO.fromFunctionM { api: Api => ZIO.fromFuture(_ => Http().bindAndHandle(api.get.routes, host, port)) }
      _       <- putStrLn(s"Server online at http://\$host:\$port/\nPress RETURN to stop...")
      _       <- getStrLn
      _       <- ZIO.fromFuture(_ => binding.unbind())
      _       <- ZIO.fromFuture(_ => system.terminate())
    } yield ()
  }.flatten

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {

    val dbLayer = DatabaseProvider.live >>> SlickItemRepository.live
    val api     = (ZLayer.succeed(ApiConfig(port)) ++ dbLayer) >>> Api.live
    val liveEnv = Console.live ++ api

    program.provideLayer(liveEnv).fold(_ => 1, _ => 0)
  }

}
