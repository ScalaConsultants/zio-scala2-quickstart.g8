package $package$

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import $package$.api._
import $package$.api.Api.{ ApiConfig, appConfigDesc, DbConfig }
import $package$.infrastructure._
import $package$.interop.slick.DatabaseProvider
import zio.console._
import zio.{ App, Has, ZIO, ZLayer, ZManaged }
import zio.logging._
import zio.logging.slf4j._
import zio.config.{ Config, config }
import zio.config.typesafe.TypesafeConfig
import akka.http.scaladsl.server.RouteConcatenation.concat

object Boot extends App {

  val program: ZIO[Console with Api $if(add_caliban_endpoint.truthy)$with GraphQLApi $endif$with Has[ActorSystem]with Config[ApiConfig], Throwable, Unit] = ZIO.effect {
    for {
      config                         <- config[ApiConfig]
      implicit0(system: ActorSystem) <- ZIO.access[Has[ActorSystem]](_.get[actor.ActorSystem])
      api                            <- ZIO.access[Api](_.get)
      $if(add_caliban_endpoint.truthy)$
      graphQLApi                     <- ZIO.access[GraphQLApi](_.get)
      $endif$
      routes                         = $if(add_caliban_endpoint.truthy)$concat(api.routes, graphQLApi.routes)$else$api.routes$endif$
      binding                        <- ZIO.fromFuture(_ => Http().bindAndHandle(routes, config.host, config.port))
      _                              <- putStrLn(s"Server online at http://\${config.host}:\${config.port}/\nPress RETURN to stop...")
      _                              <- getStrLn
      _                              <- ZIO.fromFuture(_ => binding.unbind())
    } yield ()
  }.flatten

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {

    val actorSystem = ZLayer.fromManaged(
      ZManaged.make(ZIO.effect(ActorSystem("zio-example-system")))(s => ZIO.fromFuture(_ => s.terminate()).either)
    )

    val logger = Slf4jLogger.make { (context, message) =>
    val logFormat = "[correlation-id = %s] %s"
    val correlationId = LogAnnotation.CorrelationId.render(
       context.get(LogAnnotation.CorrelationId)
     )
     logFormat.format(correlationId, message)
   }


    val configLayer = TypesafeConfig.fromDefaultLoader(appConfigDesc)

    val dbConfigLayer = configLayer.map(c => Has(new Config.Service[DbConfig] { def config = c.get.config.db }) )
    val apiConfigLayer = configLayer.map(c => Has(new Config.Service[ApiConfig] { def config = c.get.config.api }) )

    val dbLayer = ((dbConfigLayer >>> DatabaseProvider.live) ++ logger) >>> SlickItemRepository.live
    val api     = (apiConfigLayer ++ dbLayer) >>> Api.live
    $if(add_caliban_endpoint.truthy)$
    val graphQLApi = (dbLayer ++ actorSystem) >>> GraphQLApi.live
    $endif$
    val liveEnv = actorSystem ++ Console.live ++ api ++ apiConfigLayer $if(add_caliban_endpoint.truthy)$++ graphQLApi$endif$

    program.provideLayer(liveEnv).fold(_ => 1, _ => 0)
  }

}
