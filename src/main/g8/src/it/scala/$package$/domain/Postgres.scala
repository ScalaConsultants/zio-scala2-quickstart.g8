package $package$.domain

import com.dimafeng.testcontainers.PostgreSQLContainer
import zio._
import zio.blocking._
import slick.interop.zio._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import scala.jdk.CollectionConverters._

object Postgres {

  type Postgres = Has[PostgreSQLContainer]

  final case class TestCredentials(username: String, password: String)

  def postgres(
    imageName: Option[String]
  ): ZLayer[Blocking with Has[TestCredentials], Nothing, Postgres] =
    ZManaged.make {
      for {
        username <- ZIO.access[Has[TestCredentials]](_.get.username)
        password <- ZIO.access[Has[TestCredentials]](_.get.password)
        result <- effectBlocking {
                   val container = new PostgreSQLContainer(
                     dockerImageNameOverride = imageName,
                     pgUsername = Some(username),
                     pgPassword = Some(password)
                   )
                   container.start()
                   container
                 }.orDie
      } yield result
    }(conteiner => effectBlocking(conteiner.stop()).orDie).toLayer

  val postgresDbProviderLayer: ZLayer[Postgres with Has[TestCredentials], Throwable, DatabaseProvider] =
    (ZLayer
      .fromServices[PostgreSQLContainer, TestCredentials, Config] {
        case (container, credentials) =>
          val config = ConfigFactory.parseMap(
            Map(
              "url"            -> container.jdbcUrl,
              "user"           -> credentials.username,
              "password"       -> credentials.password,
              "driver"         -> "org.postgresql.Driver",
              "connectionPool" -> "HikariCP",
              "numThreads"     -> 1,
              "queueSize"      -> 100
            ).asJava
          )
          config
      } ++ ZLayer.succeed(slick.jdbc.PostgresProfile.backend)) >>> DatabaseProvider.live

}
