package $package$.domain

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.{ DockerImageName, DockerLoggerFactory }
import zio._
import zio.blocking._

object Postgres {

  final class SchemaAwarePostgresContainer(
    dockerImageNameOverride: Option[DockerImageName] = None,
    databaseName: Option[String] = None,
    pgUsername: Option[String] = None,
    pgPassword: Option[String] = None,
    mountPostgresDataToTmpfs: Boolean = false,
    currentSchema: Option[String] = None
  ) extends PostgreSQLContainer(
        dockerImageNameOverride,
        databaseName,
        pgUsername,
        pgPassword,
        mountPostgresDataToTmpfs
      ) {
    override def jdbcUrl: String =
      currentSchema.fold(super.jdbcUrl)(schema => s"\${super.jdbcUrl}&currentSchema=" + schema)
  }

  type Postgres = Has[SchemaAwarePostgresContainer]

  def postgres(currentSchema: Option[String] = None): ZLayer[Blocking, Nothing, Postgres] =
    ZManaged.make {
      effectBlocking {
        val container = new SchemaAwarePostgresContainer(
          dockerImageNameOverride = Some(DockerImageName.parse("postgres")),
          currentSchema = currentSchema
        )
        container.start()
        DockerLoggerFactory.getLogger(container.container.getDockerImageName).info(container.jdbcUrl)
        container
      }.orDie
    }(container => effectBlocking(container.stop()).orDie).toLayer

}
