package $package$

import zio._

object Layers {

  object Logger {
    import zio.logging.backend.SLF4J

    val live: ULayer[Unit] = ZLayer.make[Unit](
      Runtime.removeDefaultLoggers,
      SLF4J.slf4j
    )
  }

$if(enable_slick.truthy)$
  object Repository {
    import com.typesafe.config.Config
    import slick.interop.zio.DatabaseProvider
    import slick.jdbc.JdbcProfile
    import slick.jdbc.PostgresProfile
    import $package$.infrastructure.slick.{ SlickHealthCheckService, SlickItemRepository }
    import $package$.api.healthcheck.HealthCheckService
    import $package$.domain.ItemRepository

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
}
