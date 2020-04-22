package $package$

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation.concat
import com.typesafe.config.ConfigFactory
import $package$.api._
$if(add_caliban_endpoint.truthy)$
import $package$.api.graphql._
$endif$
import $package$.config.{ ApiConfig, AppConfig }
import $package$.infrastructure._
import $package$.interop.slick.DatabaseProvider
import zio.config.typesafe.TypesafeConfig
import zio.config.{ config, Config }
import zio.console._
import zio.{ App, Has, ZIO, ZLayer, ZManaged }
import zio.logging._
import zio.logging.slf4j._
import zio.{ App, Has, TaskLayer, ULayer, ZIO, ZLayer, ZManaged }
import zio.clock.Clock

object Boot extends App {

  val program: ZIO[Console with Api $if(add_caliban_endpoint.truthy)$with GraphQLApi $endif$with Has[ActorSystem] with Config[ApiConfig], Throwable, Unit] =
    for {
      cfg                            <- config[ApiConfig]
      implicit0(system: ActorSystem) <- ZIO.access[Has[ActorSystem]](_.get[ActorSystem])
      api                            <- ZIO.access[Api](_.get)
      $if(add_caliban_endpoint.truthy)$
      graphQLApi                     <- ZIO.access[GraphQLApi](_.get)
      $endif$
      routes                         = $if(add_caliban_endpoint.truthy)$concat(api.routes, graphQLApi.routes)$else$api.routes$endif$
      _                              <- bindAndHandle(routes, cfg.host, cfg.port).use { binding =>
                                        for {
                                          _ <- putStrLn(s"Server online at http://\${cfg.host}:\${cfg.port}/\nPress RETURN to stop...")
                                          _ <- getStrLn
                                        } yield ()
                                     }
    } yield ()

  def bindAndHandle(routes: Route, host: String, port: Int)(
    implicit system: ActorSystem
  ): ZManaged[Any, Throwable, Http.ServerBinding] =
    ZManaged.make(ZIO.fromFuture(_ => Http().bindAndHandle(routes, host, port)))(b =>
      ZIO.fromFuture(_ => b.unbind()).orDie
    )

  val loadConfig = ZIO.effect(ConfigFactory.load.resolve)

  val actorSystem = ZLayer.fromManaged(
    ZManaged.make(ZIO.effect(ActorSystem("$name$-system")))(s => ZIO.fromFuture(_ => s.terminate()).either)
  )

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
      val configLayer = TypesafeConfig.fromTypesafeConfig(rawConfig, AppConfig.descriptor)

      // using raw config since it's recommended and the simplest to work with slick
      val dbConfigLayer = ZLayer.fromEffect {
        ZIO.effect {
          rawConfig.getConfig("db")
        }
      }

      // narrowing down to the required part of the config to ensure separation of concerns
      val apiConfigLayer = configLayer.map(c => Has(c.get.api))

      val dbLayer    = ((dbConfigLayer >>> DatabaseProvider.live) ++ loggingLayer) >>> SlickItemRepository.live
      val api        = (apiConfigLayer ++ dbLayer) >>> Api.live
      $if(add_caliban_endpoint.truthy)$
      val graphQLApi = (dbLayer ++ actorSystem ++ Console.live ++ Clock.live) >>> GraphQLApi.live
      $endif$
      val liveEnv = actorSystemLayer ++ Console.live ++ api ++ apiConfigLayer$if(add_caliban_endpoint.truthy)$ ++ graphQLApi$endif$

      program.provideLayer(liveEnv)
    }.fold(_ => 1, _ => 0)

}
