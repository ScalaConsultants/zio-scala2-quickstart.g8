package $package$

import $package$.domain.ItemRepository
import $package$.infrastructure.{ Postgres, SlickItemRepository }
import $package$.infrastructure.Postgres.SchemaAwarePostgresContainer
import $package$.infrastructure.flyway.FlywayProvider
import com.typesafe.config.{ Config, ConfigFactory }
import slick.interop.zio.DatabaseProvider
import zio.blocking.Blocking
import zio.duration.durationInt
import zio.logging._
import zio.logging.slf4j.Slf4jLogger
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{ Has, Layer, ULayer, ZLayer }
$if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
import $package$.domain.Subscriber
import $package$.infrastructure.EventSubscriber
$endif$
import scala.jdk.CollectionConverters.MapHasAsJava

object ITSpec {
  type Postgres = Has[SchemaAwarePostgresContainer]
  type ITEnv    = TestEnvironment with FlywayProvider with Logging with ItemRepository
  $if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$ with Subscriber
  $endif$

  abstract class ITSpec(schema: Option[String] = None) extends RunnableSpec[ITEnv, Any] {
    type ITSpec = ZSpec[ITEnv, Any]

    override def aspects: List[TestAspect[Nothing, ITEnv, Nothing, Any]] =
      List(TestAspect.timeout(60.seconds))

    override def runner: TestRunner[ITEnv, Any] =
      TestRunner(TestExecutor.default(itLayer))

    val blockingLayer: Layer[Nothing, Blocking]       = Blocking.live
    val postgresLayer: ZLayer[Any, Nothing, Postgres] = blockingLayer >>> Postgres.postgres(schema)
    $if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
    val subscriberLayer: ZLayer[Any, Nothing, Subscriber] = Logging.ignore  >>> EventSubscriber.live.orDie
    $endif$
    val dbLayer: ZLayer[
      Any with Postgres with Blocking,
      Nothing,
      TestEnvironment with FlywayProvider with Logging with ItemRepository
    ] = {

      val config: ZLayer[Postgres, Nothing, Has[Config]] = ZLayer
        .fromService[SchemaAwarePostgresContainer, Config] { container =>
          val config = ConfigFactory.parseMap(
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
          config
        }
      val tranactor: Layer[Throwable, Has[TransactorLayer]] =
        config >>> ZLayer.fromServiceM { cfg: Config =>
          for {
            url    <- Task(cfg.getString("url"))
            user   <- Task(cfg.getString("user"))
            pwd    <- Task(cfg.getString("password"))
            driver <- Task(cfg.getString("driver"))
          } yield Transactor.fromDriverManager[Task](
            driver, // driver classname
            url,    // connect URL (driver-specific)
            user,   // user
            pwd     // password
          )
        }
      val dbProvider: ZLayer[Postgres with Any, Throwable, DatabaseProvider] =
        config ++ ZLayer.succeed(slick.jdbc.PostgresProfile.backend) >>> DatabaseProvider.live

      val flyWayProvider = config >>> FlywayProvider.live

      val postgresLayer = Postgres.postgres(schema)
      val blockingLayer = Blocking.live

      val containerDatabaseProvider: ZLayer[Blocking, Throwable, DatabaseProvider] =
        blockingLayer >>> postgresLayer >>> dbProvider

      val containerRepository: ZLayer[Blocking, Throwable, ItemRepository] =
        (Logging.ignore ++ containerDatabaseProvider) >>> SlickItemRepository.live

      val logging = Slf4jLogger.make { (context, message) =>
        val logFormat     = "[correlation-id = %s] %s"
        val correlationId = LogAnnotation.CorrelationId.render(
          context.get(LogAnnotation.CorrelationId)
        )
        logFormat.format(correlationId, message)
      }
      zio.test.environment.testEnvironment ++ flyWayProvider ++ logging ++ containerRepository
    }.orDie

     $if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
    val itLayer: ULayer[ITEnv] =
      (zio.test.environment.testEnvironment ++ postgresLayer ++ blockingLayer) >+> dbLayer ++ subscriberLayer
    $else$
    val itLayer: ULayer[ITEnv] = postgresLayer ++ blockingLayer >>> dbLayer
    $endif$
  }
}
