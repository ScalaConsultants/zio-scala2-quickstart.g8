package $package$

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import $package$.api.Api
import $package$.api.Api.{ ApiConfig, appConfigDesc, DbConfig }
import $package$.infrastructure._
import $package$.interop.slick.DatabaseProvider
import zio.console._
import zio.{ App, Has, ZIO }
import zio.config.{ Config, config }
import zio.config.typesafe.TypesafeConfig

import scala.concurrent.ExecutionContext

object Boot extends App {

  val program: ZIO[Console with Api with Config[ApiConfig], Throwable, Unit] = ZIO.effect {

    // todo: cleaner to make it a ZManaged module
    implicit val system: ActorSystem = ActorSystem("zio-example-system")

    val ec: ExecutionContext = system.dispatcher

    for {
      config  <- config[ApiConfig]
      binding <- ZIO.fromFunctionM { api: Api => ZIO.fromFuture(_ => Http().bindAndHandle(api.get.routes, config.host, config.port)) }
      _       <- putStrLn(s"Server online at http://\${config.host}:\${config.port}/\nPress RETURN to stop...")
      _       <- getStrLn
      _       <- ZIO.fromFuture(_ => binding.unbind())
      _       <- ZIO.fromFuture(_ => system.terminate())
    } yield ()
  }.flatten

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {

    val configLayer = TypesafeConfig.fromDefaultLoader(appConfigDesc)

    val dbConfigLayer = configLayer.map(c => Has(new Config.Service[DbConfig] { def config = c.get.config.db }) )
    val apiConfigLayer = configLayer.map(c => Has(new Config.Service[ApiConfig] { def config = c.get.config.api }) )

    val dbLayer = dbConfigLayer >>> DatabaseProvider.live >>> SlickItemRepository.live
    val api     = (apiConfigLayer ++ dbLayer) >>> Api.live
    val liveEnv = Console.live ++ api ++ apiConfigLayer

    program.provideLayer(liveEnv).fold(_ => 1, _ => 0)
  }

}
