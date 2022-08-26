package $package$

import com.typesafe.config.{ Config, ConfigFactory }
import slick.jdbc.PostgresProfile
import slick.interop.zio.DatabaseProvider

import zio._
import zio.test.ZIOSpecDefault
import zio.logging.backend.SLF4J

import $package$.domain.ItemRepository
import $package$.infrastructure.{ Postgres, SlickItemRepository }
import $package$.infrastructure.Postgres.SchemaAwarePostgresContainer
import $package$.infrastructure.flyway.FlywayProvider

import scala.jdk.CollectionConverters.MapHasAsJava

abstract class ITSpec(schema: Option[String]) extends ZIOSpecDefault {

  val itLayers: ZLayer[Scope, Throwable, FlywayProvider with ItemRepository] = {

    val logging: ULayer[Unit] = ZLayer.make[Unit](
      Runtime.removeDefaultLoggers,
      SLF4J.slf4j
    )

    val postgres: URLayer[Scope, SchemaAwarePostgresContainer] = Postgres.postgres(schema)

    val config: URLayer[SchemaAwarePostgresContainer, Config] = ZLayer {
      for {
        container <- ZIO.service[SchemaAwarePostgresContainer]
      } yield ConfigFactory.parseMap(
        Map(
          "url"            -> container.jdbcUrl,
          "user"           -> container.username,
          "password"       -> container.password,
          "driver"         -> "org.postgresql.Driver",
          "connectionPool" -> "HikariCP",
          "numThreads"     -> 1,
          "queueSize"      -> 100
        ).asJava
      )
    }

    val jdbcProfile: ULayer[PostgresProfile] = ZLayer.succeed(PostgresProfile)

    ZLayer.makeSome[Scope, FlywayProvider with ItemRepository](
      logging,
      jdbcProfile,
      config,
      postgres,
      DatabaseProvider.live,
      SlickItemRepository.live,
      FlywayProvider.live
    )
  }

}
