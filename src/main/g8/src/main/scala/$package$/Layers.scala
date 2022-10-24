package $package$

$if(enable_slick.truthy)$
import com.typesafe.config.Config
import slick.interop.zio.DatabaseProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile
$endif$
$if(enable_quill.truthy)$
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.getquill.jdbczio.Quill
import io.getquill.Literal
import javax.sql.DataSource
import scala.jdk.CollectionConverters.MapHasAsJava
$endif$

import zio._
import zio.logging.backend.SLF4J

$if(enable_slick.truthy)$
import $package$.infrastructure.slick.{SlickHealthCheckService, SlickItemRepository}
$endif$
$if(enable_quill.truthy)$
import $package$.infrastructure.quill.{QuillHealthCheckService, QuillItemRepository}
$endif$
import $package$.api.healthcheck.HealthCheckService
import $package$.domain.ItemRepository

object Layers {

  object Logger {

    val live: ULayer[Unit] = ZLayer.make[Unit](
      Runtime.removeDefaultLoggers,
      SLF4J.slf4j
    )
  }

$if(enable_slick.truthy)$
  object Repository {

    val jdbcProfileLayer: ULayer[JdbcProfile] = ZLayer.succeed[JdbcProfile](PostgresProfile)

    val dbConfigLayer: URLayer[Config, Config] = ZLayer {
      for {
        config <- ZIO.service[Config]
        dbConfig = config.getConfig("db")
      } yield dbConfig
    }

    val slickLayer: URLayer[Config, DatabaseProvider] =
      (jdbcProfileLayer ++ dbConfigLayer) >>> DatabaseProvider.live.orDie

    val itemRepository: RLayer[Config, ItemRepository] = (slickLayer >>> SlickItemRepository.live).orDie
    val healthCheckService: RLayer[Config, HealthCheckService] = (slickLayer >>> SlickHealthCheckService.live).orDie
  }
$endif$
$if(enable_quill.truthy)$
  object Repository {

    val quillDataSourceLayer: RLayer[Config, DataSource] = ZLayer {
      for {
        config <- ZIO.service[Config]
        dbConfig = config.getConfig("db")
      } yield Quill.DataSource.fromConfig(
        ConfigFactory.parseMap(
          Map(
            "dataSourceClassName" -> "org.postgresql.ds.PGSimpleDataSource",
            "dataSource.url" -> dbConfig.getString("url"),
            "dataSource.user" -> dbConfig.getString("user"),
            "dataSource.password" -> dbConfig.getString("password")
          ).asJava
        )
      )
    }.flatten.orDie

    val quillPostgresLayer: RLayer[DataSource, Quill.Postgres[Literal]] = Quill.Postgres.fromNamingStrategy(Literal)

    val quillLayer: RLayer[Config, Quill.Postgres[Literal]] = (quillDataSourceLayer >>> quillPostgresLayer).orDie

    val itemRepository: RLayer[Config, ItemRepository] = (quillLayer >>> QuillItemRepository.live).orDie
    val healthCheckService: RLayer[Config, HealthCheckService] = (quillLayer >>> QuillHealthCheckService.live).orDie
  }
$endif$
$if(!enable_quill.truthy&&!enable_slick.truthy)$
  object Repository {
    import $package$.api.healthcheck.{HealthCheckService, InMemoryHealthCheckService}
    import $package$.domain.{ItemRepository, InMemoryItemRepository}

    val itemRepository: ULayer[ItemRepository] = InMemoryItemRepository.live
    val healthCheckService: ULayer[HealthCheckService] = InMemoryHealthCheckService.live
  }
$endif$
}
