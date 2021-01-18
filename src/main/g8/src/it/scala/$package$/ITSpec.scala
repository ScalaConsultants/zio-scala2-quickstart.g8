package $package$

import $package$.domain.ItemRepository
import $package$.infrastructure._
import $package$.infrastructure.Postgres.SchemaAwarePostgresContainer
import $package$.infrastructure.flyway.FlywayProvider
import com.typesafe.config.{ Config, ConfigFactory }
import zio.blocking.Blocking
import zio.duration.durationInt
$if(slick.truthy)$
import slick.interop.zio.DatabaseProvider
$endif$
$if(doobie.truthy)$
import com.example.infrastructure.utilities.TransactorLayer
$endif$
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


    val subscriberLayer: ZLayer[Any, Nothing, Subscriber] = Logging.ignore >>> EventSubscriber.live.orDie

    val dbLayer: ZLayer[
      Any,
      Nothing,
      _root_.zio.test.environment.TestEnvironment with FlywayProvider with Logging with ItemRepository
    ] = {
      val blockingLayer: ULayer[Blocking] = Blocking.live
      val postgresLayer: ULayer[Postgres] = blockingLayer >>> Postgres.postgres(schema)

      val config: ULayer[Has[Config]]                      = postgresLayer >>> ZLayer
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
      $if(doobie.truthy)$
      val tranactor: Layer[Throwable, Has[TransactorLayer]] =
      config >>> DoobieDatabaseProvider.tranactorLayer


      val doobieWithContainerRepository: Layer[Throwable, ItemRepository] =
        (tranactor ++ Logging.ignore) >>> DoobieItemRepository.live

      $endif$
      $if(slick.truthy)$
      val dbProvider: Layer[Throwable, DatabaseProvider] =
        postgresLayer >>> config ++ ZLayer.succeed(slick.jdbc.PostgresProfile.backend) >>> DatabaseProvider.live

      val slickWithContainerRepository: Layer[Throwable, ItemRepository] =
        (Logging.ignore ++ dbProvider) >>> SlickItemRepository.live
      $endif$

      val logging = Slf4jLogger.make { (context, message) =>
        val logFormat     = "[correlation-id = %s] %s"
        val correlationId = LogAnnotation.CorrelationId.render(
          context.get(LogAnnotation.CorrelationId)
        )
        logFormat.format(correlationId, message)
      }

      val flyWayProvider = config >>> FlywayProvider.live

      zio.test.environment.testEnvironment ++ flyWayProvider ++ logging      $if(doobie.truthy)$ ++ doobieWithContainerRepository   $endif$     $if(slick.truthy)$ ++ slickWithContainerRepository   $endif$
    }.orDie

     $if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
    val itLayer: ULayer[ITEnv] =
      dbLayer ++ subscriberLayer
    $else$
    val itLayer: ULayer[ITEnv] = dbLayer
    $endif$
  }
}
