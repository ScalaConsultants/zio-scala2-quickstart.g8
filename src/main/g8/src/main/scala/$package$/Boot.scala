package $package$

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import $package$.api.Api
import $package$.api.Api.{ ApiConfig, configDescr }
import $package$.infrastructure._
import $package$.interop.slick.DatabaseProvider
import zio.console._
import zio.{ App, ZIO }
import zio.config.Config
import zio.config.typesafe.TypesafeConfig

import scala.concurrent.ExecutionContext

object Boot extends App {

  val program: ZIO[Console with Api with Config[ApiConfig], Throwable, Unit] = ZIO.effect {

    // todo: cleaner to make it a ZManaged module
    implicit val system: ActorSystem = ActorSystem("zio-example-system")

    val ec: ExecutionContext = system.dispatcher

    for {
      config  <- ZIO.access[Config[ApiConfig]](_.get.config)
      binding <- ZIO.fromFunctionM { api: Api => ZIO.fromFuture(_ => Http().bindAndHandle(api.get.routes, config.host, config.port)) }
      _       <- putStrLn(s"Server online at http://\${config.host}:\${config.port}/\nPress RETURN to stop...")
      _       <- getStrLn
      _       <- ZIO.fromFuture(_ => binding.unbind())
      _       <- ZIO.fromFuture(_ => system.terminate())
    } yield ()
  }.flatten

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {

    val dbLayer = DatabaseProvider.live >>> SlickItemRepository.live
    val configLayer = TypesafeConfig.fromHoconFile(configDescr, new File("src/main/resources/application.conf"))
    val api     = (configLayer ++ dbLayer) >>> Api.live
    val liveEnv = Console.live ++ api ++ configLayer

    program.provideLayer(liveEnv).fold(_ => 1, _ => 0)
  }

}
