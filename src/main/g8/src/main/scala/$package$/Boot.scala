package $package$

import akka.actor.ActorSystem
import $package$.api._
$if(add_caliban_endpoint.truthy)$
import $package$.api.graphql.GraphQLApi
$endif$
import $package$.config.AppConfig
import $package$.domain.ItemRepository
import $package$.infrastructure._
import $package$.interop.slick.DatabaseProvider
import $package$.server.HttpServer
import com.typesafe.config.{ Config, ConfigFactory }
import zio.clock.Clock
import zio.config.typesafe.TypesafeConfig
import zio.console._
import zio.logging._
import zio.logging.slf4j._
import zio._

object Boot extends App {

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    ZIO(ConfigFactory.load.resolve)
      .flatMap(rawConfig => program.provideCustomLayer(prepareEnvironment(rawConfig)))
      .as(0)
      .catchAll(error => putStrLn(error.getMessage).as(1))

  private val program: ZIO[HttpServer with Console, Throwable, Unit] =
    HttpServer.start.use(_ => putStrLn(s"Server online. Press RETURN to stop...") <* getStrLn)

  private def prepareEnvironment(rawConfig: Config): TaskLayer[HttpServer] = {
    val configLayer = TypesafeConfig.fromTypesafeConfig(rawConfig, AppConfig.descriptor)

    // using raw config since it's recommended and the simplest to work with slick
    val dbConfigLayer = ZLayer.fromEffect(ZIO(rawConfig.getConfig("db")))

    // narrowing down to the required part of the config to ensure separation of concerns
    val apiConfigLayer = configLayer.map(c => Has(c.get.api))

    val actorSystemLayer: TaskLayer[Has[ActorSystem]] = ZLayer.fromManaged {
      ZManaged.make(ZIO(ActorSystem("zio-akka-quickstart-system")))(s => ZIO.fromFuture(_ => s.terminate()).either)
    }

    val loggingLayer: ULayer[Logging] = Slf4jLogger.make { (context, message) =>
      val logFormat = "[correlation-id = %s] %s"
      val correlationId = LogAnnotation.CorrelationId.render(
        context.get(LogAnnotation.CorrelationId)
      )
      logFormat.format(correlationId, message)
    }

    val dbLayer: TaskLayer[ItemRepository] =
      ((dbConfigLayer >>> DatabaseProvider.live) ++ loggingLayer) >>> SlickItemRepository.live

    val apiLayer: TaskLayer[Api] = (apiConfigLayer ++ dbLayer) >>> Api.live

    $if(add_caliban_endpoint.truthy)$
    val graphQLApiLayer: TaskLayer[GraphQLApi] =
      (dbLayer ++ actorSystemLayer ++ loggingLayer ++ Clock.live) >>> GraphQLApi.live
    $endif$

    (actorSystemLayer ++ apiConfigLayer ++ apiLayer $if(add_caliban_endpoint.truthy)$ ++ graphQLApiLayer$endif$) >>> HttpServer.live
  }
}
