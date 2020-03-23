package $package$

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import $package$.api.Api
import $package$.config.{ ApiConfig, AppConfig }
import $package$.infrastructure._
import $package$.interop.slick.{ DatabaseConfig, DatabaseProvider }
import com.typesafe.config.ConfigFactory
import zio.console._
import zio.logging._
import zio.logging.slf4j._
import zio.{ App, Has, TaskLayer, ULayer, ZIO, ZLayer, ZManaged }
import zio.config.{ config, Config }
import zio.config.typesafe.TypesafeConfig

object Boot extends App {

  val program: ZIO[Console with Api with Has[ActorSystem] with Config[ApiConfig], Throwable, Unit] = ZIO.effect {

    for {
      cfg                            <- config[ApiConfig]
      implicit0(system: ActorSystem) <- ZIO.access[Has[ActorSystem]](_.get[ActorSystem])
      api                            <- ZIO.access[Api](_.get)
      binding                        <- ZIO.fromFuture(_ => Http().bindAndHandle(api.routes, cfg.host, cfg.port))
      _                              <- putStrLn(s"Server online at http://\${cfg.host}:\${cfg.port}/\nPress RETURN to stop...")
      _                              <- getStrLn
      _                              <- ZIO.fromFuture(_ => binding.unbind())
      _                              <- ZIO.fromFuture(_ => system.terminate())
    } yield ()
  }.flatten

  val loadConfig = ZIO.effect(ConfigFactory.load.resolve)

  val loggingLayer: ULayer[Logging] = Slf4jLogger.make { (context, message) =>
    val logFormat = "[correlation-id = %s] %s"
    val correlationId = LogAnnotation.CorrelationId.render(
      context.get(LogAnnotation.CorrelationId)
    )
    logFormat.format(correlationId, message)
  }

  val actorSystemLayer: TaskLayer[Has[ActorSystem]] = ZLayer.fromManaged(
    ZManaged.make(ZIO.effect(ActorSystem("zio-example-system")))(s => ZIO.fromFuture(_ => s.terminate()).either)
  )

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    loadConfig.flatMap { rawConfig =>
      val configLayer = TypesafeConfig.fromHocon(rawConfig, AppConfig.description)

      // using raw config since it's recommended and the simplest to work with slick
      val dbConfigLayer = ZLayer.fromEffect(ZIO.effect {
        val dbc = DatabaseConfig(rawConfig.getConfig("db"))
        new Config.Service[DatabaseConfig] { def config = dbc }
      })
      // narrowing down to the required part of the config to ensure separation of concerns
      val apiConfigLayer = configLayer.map(c => Has(new Config.Service[ApiConfig] { def config = c.get.config.api }))

      val dbLayer = ((dbConfigLayer >>> DatabaseProvider.live) ++ loggingLayer) >>> SlickItemRepository.live
      val api     = (apiConfigLayer ++ dbLayer) >>> Api.live
      val liveEnv = actorSystemLayer ++ Console.live ++ api ++ apiConfigLayer

      program.provideLayer(liveEnv)
    }.fold(_ => 1, _ => 0)

}
