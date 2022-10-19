package $package$

import com.typesafe.config.{ Config, ConfigFactory }

import zio._
import zio.test.ZIOSpecDefault
import zio.logging.backend.SLF4J

import $package$.domain.ItemRepository
import $package$.infrastructure.slick.SlickItemRepository
import $package$.infrastructure.Postgres
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
          "db.url"            -> container.jdbcUrl,
          "db.user"           -> container.username,
          "db.password"       -> container.password,
          "db.driver"         -> "org.postgresql.Driver",
          "db.connectionPool" -> "HikariCP",
          "db.numThreads"     -> 1,
          "db.queueSize"      -> 100
        ).asJava
      )
    }

    object DatabaseInfra {
      import slick.jdbc.PostgresProfile
      import slick.interop.zio.DatabaseProvider

      val jdbcProfileLayer: ULayer[PostgresProfile] = ZLayer.succeed(PostgresProfile)

      val dbConfigLayer: RLayer[SchemaAwarePostgresContainer, Config] = config.flatMap { rawConfig =>
        ZLayer.succeed(rawConfig.get.getConfig("db"))
      }

      val live: RLayer[SchemaAwarePostgresContainer, DatabaseProvider] =
        (jdbcProfileLayer ++ dbConfigLayer) >>> DatabaseProvider.live.orDie
    }

    ZLayer.makeSome[Scope, FlywayProvider with ItemRepository](
      logging,
      config,
      postgres,
      DatabaseInfra.live,
      SlickItemRepository.live,
      FlywayProvider.live
    )
  }

}
