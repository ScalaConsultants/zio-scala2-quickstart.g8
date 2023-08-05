package $package$

$if(enable_slick.truthy) $
import com.typesafe.config.{ Config, ConfigFactory }
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import slick.interop.zio.DatabaseProvider
import slick.jdbc.{ JdbcProfile, PostgresProfile }
import slick.util.ConfigExtensionMethods._
$endif$
$if(enable_quill.truthy) $
import io.getquill.Literal
import io.getquill.jdbczio.Quill
$endif$
import zio._
import zio.logging.backend.SLF4J

$if(enable_slick.truthy) $
import $package$.infrastructure.slick.{ SlickHealthCheckService, SlickItemRepository }
$endif$
$if(enable_quill.truthy) $
import $package$.infrastructure.quill.{ QuillHealthCheckService, QuillItemRepository }
$endif$
$if(!enable_quill.truthy &&! enable_slick.truthy) $
import $package$.api.healthcheck.InMemoryHealthCheckService
import $package$.domain.InMemoryItemRepository
$endif$
import $package$.infrastructure.flyway.FlywayProvider
import $package$.api.healthcheck.HealthCheckService
import $package$.domain.ItemRepository

object Layers {

  val logger: ULayer[Unit] = ZLayer.make[Unit](
    Runtime.removeDefaultLoggers,
    SLF4J.slf4j
  )

  $if(enable_slick.truthy) $
  private val jdbcProfileLayer: ULayer[JdbcProfile] = ZLayer.succeed[JdbcProfile](PostgresProfile)

  private val dbConfigLayer: ULayer[Config] =
    ZLayer {
      ZIO
        .attempt(ConfigFactory.load.resolve)
        .orDie
        .map(_.getConfig("dataSource"))
    }

  private val dataSourceLayer = dbConfigLayer.project[HikariDataSource] { config =>
    new HikariDataSource(
      new HikariConfig(config.toProperties)
    )
  }

  private val slickLayer: ULayer[DatabaseProvider] =
    (jdbcProfileLayer ++ dataSourceLayer) >>> DatabaseProvider.fromDataSource().orDie

  val itemRepository: ULayer[ItemRepository] = (slickLayer >>> SlickItemRepository.live).orDie

  val healthCheckService: ULayer[HealthCheckService] = (slickLayer >>> SlickHealthCheckService.live).orDie

  val flyway: ULayer[FlywayProvider] = (dataSourceLayer >>> FlywayProvider.live).orDie
  $endif$
  $if(enable_quill.truthy) $
  private val dataSourceLayer        = Quill.DataSource.fromPrefix("db")

  private val postgresLayer = Quill.Postgres.fromNamingStrategy(Literal)

  val itemRepository: ULayer[ItemRepository] = (dataSourceLayer >>> postgresLayer >>> QuillItemRepository.live).orDie

  val healthCheckService: ULayer[HealthCheckService] =
    (dataSourceLayer >>> postgresLayer >>> QuillHealthCheckService.live).orDie

  val flyway: ULayer[FlywayProvider]         = (dataSourceLayer >>> FlywayProvider.live).orDie
  $endif$
  $if(!enable_quill.truthy &&! enable_slick.truthy) $
  val itemRepository: ULayer[ItemRepository] = InMemoryItemRepository.live

  val healthCheckService: ULayer[HealthCheckService] = InMemoryHealthCheckService.live
  $endif$
}
