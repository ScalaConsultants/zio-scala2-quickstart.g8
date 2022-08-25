package $package$

import akka.actor.ActorSystem
import akka.http.interop._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.{ Config, ConfigFactory }
import slick.interop.zio.DatabaseProvider
import slick.jdbc.JdbcProfile

import zio._
import zio.config.ReadError
import zio.config.typesafe.TypesafeConfig
import zio.logging.backend.SLF4J

import $package$.api._
import $package$.application.ApplicationService
import $package$.config.AppConfig
import $package$.infrastructure._
import $package$.infrastructure.flyway.FlywayProvider

object Boot extends ZIOAppDefault {

  def run: RIO[Scope, Nothing] = program.provideSomeLayer(Layers.live)

  private val program: RIO[Scope with FlywayProvider with HttpServer, Nothing] = {

    val startHttpServer: RIO[Scope with HttpServer, Http.ServerBinding] =
      HttpServer.start.tap(_ => Console.printLine("Server online."))

    val migrateDbSchema: RIO[FlywayProvider, Unit] =
      FlywayProvider.flyway
        .flatMap(_.migrate)
        .retry(Schedule.exponential(200.millis))
        .flatMap(res => Console.printLine(s"Flyway migration completed with: \$res"))

    startHttpServer *>
    migrateDbSchema *>
    ZIO.never
  }

  object Layers {
    val live: ZLayer[Scope, Throwable, HttpServer with FlywayProvider] =
      ZLayer.makeSome[Scope, HttpServer with FlywayProvider](
        ApiConfig.live,
        DBConfig.live,
        AkkaActorSystem.live,
        JdbcProfile.live,
        DatabaseProvider.live,
        SlickHealthCheckService.live,
        SlickItemRepository.live,
        ApplicationService.live,
        Api.live,
        ApiRoutes.live,
        HttpServer.live,
        FlywayProvider.live,
        Logger.live
      )
  }

  object Config {
    val effect: Task[Config] = ZIO.attempt(ConfigFactory.load.resolve)
  }

  object ApiConfig {
    val live: Layer[ReadError[String], HttpServer.Config] =
      TypesafeConfig
        .fromTypesafeConfig[AppConfig](Config.effect, AppConfig.descriptor)
        .flatMap(c => ZLayer.succeed(c.get.api))
  }

  object DBConfig {
    val live: TaskLayer[Config] =
      // using raw config since it's recommended and the simplest to work with slick
      ZLayer.fromZIO {
        for {
          config <- Config.effect
        } yield config.getConfig("db")
      }
  }

  object AkkaActorSystem {
    val live: RLayer[Scope, ActorSystem] = {
      val effect = ZIO.acquireRelease(ZIO.attempt(ActorSystem("$name$-system")))(actorSystem =>
        ZIO.fromFuture(_ => actorSystem.terminate()).either
      )

      ZLayer.fromZIO(effect)
    }
  }

  object ApiRoutes {
    val live: URLayer[Api, Route] = ZLayer {
      for {
        api <- ZIO.service[Api]
      } yield api.routes
    }
  }

  object JdbcProfile {
    val live: ULayer[JdbcProfile] = ZLayer.succeed[JdbcProfile](slick.jdbc.PostgresProfile)
  }

  object Logger {
    val live: ULayer[Unit] = ZLayer.make[Unit](
      Runtime.removeDefaultLoggers,
      SLF4J.slf4j
    )
  }
}
