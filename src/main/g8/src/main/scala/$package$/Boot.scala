package $package$

import akka.actor.ActorSystem
import akka.http.interop._
$if(add_caliban_endpoint.truthy)$
import akka.http.scaladsl.server.RouteConcatenation._
$endif$
import akka.http.scaladsl.server.Route
import com.typesafe.config.{ Config, ConfigFactory }
import slick.interop.zio.DatabaseProvider
$if(add_caliban_endpoint.truthy)$
import zio.clock.Clock
$endif$
import zio.config.typesafe.TypesafeConfig
import zio.console._
import zio.logging._
import zio.logging.slf4j._
import zio._
import $package$.api._
$if(add_caliban_endpoint.truthy)$
import $package$.api.graphql.GraphQLApi
$endif$
import $package$.config.AppConfig
import $package$.domain.ItemRepository
import $package$.infrastructure._

object Boot extends App {

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    ZIO(ConfigFactory.load.resolve)
      .flatMap(rawConfig => program.provideCustomLayer(prepareEnvironment(rawConfig)))
      .as(ExitCode.success)
      .catchAll(error => putStrLn(error.getMessage).as(ExitCode.failure))

  private val program: ZIO[HttpServer with Console, Throwable, Unit] =
    HttpServer.start.tapM(_ => putStrLn(s"Server online.")).useForever

  private def prepareEnvironment(rawConfig: Config): TaskLayer[HttpServer] = {
    val configLayer = TypesafeConfig.fromTypesafeConfig(rawConfig, AppConfig.descriptor)

    // using raw config since it's recommended and the simplest to work with slick
    val dbConfigLayer = ZLayer.fromEffect(ZIO(rawConfig.getConfig("db")))
    val dbBackendLayer = ZLayer.succeed(slick.jdbc.H2Profile.backend)

    // narrowing down to the required part of the config to ensure separation of concerns
    val apiConfigLayer = configLayer.map(c => Has(c.get.api))

    val actorSystemLayer: TaskLayer[Has[ActorSystem]] = ZLayer.fromManaged {
      ZManaged.make(ZIO(ActorSystem("$name$-system")))(s => ZIO.fromFuture(_ => s.terminate()).either)
    }

    val loggingLayer: ULayer[Logging] = Slf4jLogger.make { (context, message) =>
      val logFormat = "[correlation-id = %s] %s"
      val correlationId = LogAnnotation.CorrelationId.render(
        context.get(LogAnnotation.CorrelationId)
      )
      logFormat.format(correlationId, message)
    }

    val dbLayer: TaskLayer[ItemRepository] =
      (((dbConfigLayer ++ dbBackendLayer) >>> DatabaseProvider.live) ++ loggingLayer) >>> SlickItemRepository.live

    val apiLayer: TaskLayer[Api] = (apiConfigLayer ++ dbLayer$if(add_websocket_endpoint.truthy)$ ++ actorSystemLayer$endif$) >>> Api.live

    $if(add_caliban_endpoint.truthy)$
    val graphQLApiLayer: TaskLayer[GraphQLApi] =
      (dbLayer ++ actorSystemLayer ++ loggingLayer ++ Clock.live) >>> GraphQLApi.live
    $endif$

    val routesLayer: ZLayer[Api$if(add_caliban_endpoint.truthy)$ with GraphQLApi$endif$, Nothing, Has[Route]] =
    $if(add_caliban_endpoint.truthy)$
      ZLayer.fromServices[Api.Service, api.graphql.GraphQLApi.Service, Route]{ (api, gApi) => api.routes ~ gApi.routes }
    $else$
      ZLayer.fromService(_.routes)
    $endif$

    (actorSystemLayer ++ apiConfigLayer ++ (apiLayer $if(add_caliban_endpoint.truthy)$ ++ graphQLApiLayer$endif$ >>> routesLayer)) >>> HttpServer.live
  }
}
