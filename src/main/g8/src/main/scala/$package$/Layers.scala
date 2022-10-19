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
  object DatabaseInfra {

    import com.typesafe.config.Config
    import slick.interop.zio.DatabaseProvider
    import slick.jdbc.JdbcProfile
    import slick.jdbc.PostgresProfile
    import $package$.config.AppConfig.RawConfig

    val jdbcProfileLayer: ULayer[JdbcProfile] = ZLayer.succeed[JdbcProfile](PostgresProfile)

    val dbConfigLayer: ULayer[Config] = RawConfig.live.flatMap { rawConfig =>
      ZLayer.succeed(rawConfig.get.getConfig("db"))
    }

    val live: ULayer[DatabaseProvider] = (jdbcProfileLayer ++ dbConfigLayer) >>> DatabaseProvider.live.orDie

  }

  object Repository {
    import slick.interop.zio.DatabaseProvider
    import $package$.infrastructure.slick.{ SlickHealthCheckService, SlickItemRepository }
    import $package$.api.healthcheck.HealthCheckService
    import $package$.domain.ItemRepository

    val itemRepository: RLayer[DatabaseProvider, ItemRepository] = SlickItemRepository.live
    val healthCheckService: RLayer[DatabaseProvider, HealthCheckService] = SlickHealthCheckService.live
  }
$endif$

}
