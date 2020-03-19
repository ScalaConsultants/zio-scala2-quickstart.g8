package $package$

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import $package$.api.Api
import $package$.api.Api.{ appConfigDesc, ApiConfig, DbConfig }
import $package$.infrastructure._
import $package$.interop.slick.DatabaseProvider
import com.typesafe.config.ConfigFactory
import zio.console._
import zio.logging._
import zio.logging.slf4j._
import zio.{ App, Has, ZIO, ZLayer }
import zio.config.{ config, Config }
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

  val loadConfig = ZIO.effect(ConfigFactory.load.resolve)

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    loadConfig.flatMap { rawConfig =>
      val logger = Slf4jLogger.make { (context, message) =>
        val logFormat = "[correlation-id = %s] %s"
        val correlationId = LogAnnotation.CorrelationId.render(
          context.get(LogAnnotation.CorrelationId)
        )
        logFormat.format(correlationId, message)
      }

      val configLayer = TypesafeConfig.fromHocon(rawConfig, appConfigDesc)

      // using raw config since it's recommended and the simplest to work with slick
      val dbConfigLayer = ZLayer.fromEffect(ZIO.effect {
        val dbc = DbConfig(rawConfig.getConfig("db"))
        new Config.Service[DbConfig] { def config = dbc }
      })
      // narrowing down to the required part of the config to ensure separation of concerns
      val apiConfigLayer = configLayer.map(c => Has(new Config.Service[ApiConfig] { def config = c.get.config.api }))

      val dbLayer = ((dbConfigLayer >>> DatabaseProvider.live) ++ logger) >>> SlickItemRepository.live
      val api     = (apiConfigLayer ++ dbLayer) >>> Api.live
      val liveEnv = Console.live ++ api ++ apiConfigLayer

      program.provideLayer(liveEnv)
    }.fold(_ => 1, _ => 0)

}
