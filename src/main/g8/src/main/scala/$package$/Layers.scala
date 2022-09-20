package $package$

import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile

import zio._
import zio.logging.backend.SLF4J

object Layers {

  object JdbcProfile {
    val live: ULayer[JdbcProfile] = ZLayer.succeed[JdbcProfile](PostgresProfile)
  }

  object Logger {
    val live: ULayer[Unit] = ZLayer.make[Unit](
      Runtime.removeDefaultLoggers,
      SLF4J.slf4j
    )
  }

}
