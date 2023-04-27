package $package$.infrastructure

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.{ DockerImageName, DockerLoggerFactory }

import zio._

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
    override def jdbcUrl: String = currentSchema.fold(super.jdbcUrl) { schema =>
      s"\${super.jdbcUrl}&currentSchema=" + schema
    }
  }

  def postgres(currentSchema: Option[String] = None): ULayer[SchemaAwarePostgresContainer] = {

    val acquire: UIO[SchemaAwarePostgresContainer] = ZIO.attemptBlocking {

      val container: SchemaAwarePostgresContainer = new SchemaAwarePostgresContainer(
        dockerImageNameOverride = Some(DockerImageName.parse("postgres")),
        currentSchema = currentSchema
      )

      container.start()

      DockerLoggerFactory
        .getLogger(container.container.getDockerImageName)
        .info(container.jdbcUrl)

      container

    }.orDie

    ZLayer.scoped {
      ZIO.acquireRelease(acquire) { container =>
        ZIO.attemptBlocking(container.stop()).orDie
      }
    }

  }

}
