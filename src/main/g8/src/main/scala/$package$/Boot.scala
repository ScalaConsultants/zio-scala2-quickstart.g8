package $package$

import akka.actor.ActorSystem
import akka.http.interop._
$if(add_caliban_endpoint.truthy)$
import akka.http.scaladsl.server.RouteConcatenation._
$endif$
import akka.http.scaladsl.server.Route
import com.typesafe.config.{ Config, ConfigFactory }
import slick.interop.zio.DatabaseProvider
import slick.jdbc.JdbcProfile
$if(add_caliban_endpoint.truthy)$
import zio.clock.Clock
$endif$
import zio.config.typesafe.TypesafeConfig
import zio.console._
import zio.duration._
import zio.logging._
import zio.logging.slf4j._
import zio._
import $package$.api._
import $package$.api.healthcheck.HealthCheckService
$if(add_caliban_endpoint.truthy)$
import $package$.api.graphql.GraphQLApi
$endif$
import $package$.application.ApplicationService
import $package$.config.AppConfig
import $package$.domain.ItemRepository
$if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
import $package$.domain.Subscriber
$endif$
import $package$.infrastructure._
import $package$.infrastructure.flyway.FlywayProvider

object Boot extends App {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    ZIO(ConfigFactory.load.resolve)
      .flatMap(rawConfig => program.provideCustomLayer(prepareEnvironment(rawConfig)))
      .exitCode

  private val program: RIO[HttpServer with Has[FlywayProvider] with ZEnv, Unit] = {
    val startHttpServer =
      HttpServer.start.tapM(_ => putStrLn("Server online."))

    val migrateDbSchema =
      FlywayProvider.flyway
        .flatMap(_.migrate)
        .retry(Schedule.exponential(200.millis))
        .flatMap(res => putStrLn(s"Flyway migration completed with: \$res"))
        .toManaged_

    (startHttpServer *> migrateDbSchema).useForever
  }

  private def prepareEnvironment(rawConfig: Config): TaskLayer[HttpServer with Has[FlywayProvider]] = {
    val configLayer = TypesafeConfig.fromTypesafeConfig(rawConfig, AppConfig.descriptor)

    // using raw config since it's recommended and the simplest to work with slick
    val dbConfigLayer  = ZLayer.fromEffect(ZIO(rawConfig.getConfig("db")))
    val dbBackendLayer = ZLayer.succeed[JdbcProfile](slick.jdbc.PostgresProfile)

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

    $if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
    val subscriberLayer: TaskLayer[Has[Subscriber]] = 
      loggingLayer >>> EventSubscriber.live
    $endif$

    val dbProvider: ZLayer[Any, Throwable, Has[DatabaseProvider]] =
      (dbConfigLayer ++ dbBackendLayer) >>> DatabaseProvider.live

    val dbLayer: TaskLayer[Has[ItemRepository]] =
      (dbProvider ++ loggingLayer) >>> SlickItemRepository.live

    val healthCheckLayer: TaskLayer[Has[HealthCheckService]] =
      dbProvider >>> SlickHealthCheckService.live
      
    val flywayLayer: TaskLayer[Has[FlywayProvider]] = 
      dbConfigLayer >>> FlywayProvider.live

    $if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
    val applicationLayer: ZLayer[Any, Throwable, Has[ApplicationService]] = 
      (dbLayer ++ subscriberLayer) >>> ApplicationService.live

    $else$
    val applicationLayer: ZLayer[Any, Throwable, Has[ApplicationService]] = 
      dbLayer >>> ApplicationService.live
    $endif$

    val apiLayer: TaskLayer[Has[Api]] = 
      (apiConfigLayer ++ applicationLayer ++ actorSystemLayer ++ healthCheckLayer ++ loggingLayer) >>> Api.live

    $if(add_caliban_endpoint.truthy)$
    val graphQLApiLayer: TaskLayer[Has[GraphQLApi]] =
      (applicationLayer ++ actorSystemLayer ++ loggingLayer ++ Clock.live) >>> GraphQLApi.live
    $endif$

    val routesLayer: URLayer[Has[Api]$if(add_caliban_endpoint.truthy)$ with Has[GraphQLApi]$endif$, Has[Route]] =
    $if(add_caliban_endpoint.truthy)$
      ZLayer.fromServices[Api, GraphQLApi, Route] { (api, gApi) =>
        api.routes ~ gApi.routes
      }

    $else$
      ZLayer.fromService(_.routes)
    $endif$

    val serverEnv: TaskLayer[HttpServer] =
      (actorSystemLayer ++ apiConfigLayer ++ (apiLayer$if(add_caliban_endpoint.truthy)$ ++ graphQLApiLayer$endif$ >>> routesLayer)) >>> HttpServer.live

    serverEnv ++ flywayLayer
  }
}
