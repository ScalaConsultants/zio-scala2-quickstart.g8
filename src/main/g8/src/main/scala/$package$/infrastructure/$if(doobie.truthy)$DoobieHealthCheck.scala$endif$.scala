package $package$.infrastructure

import $package$.domain.{DbStatus, HealthCheck, RepositoryError}
import $package$.infrastructure.utilities.TransactorLayer
import doobie.implicits._
import zio._
import zio.interop.catz.taskConcurrentInstance

object DoobieHealthCheck {

  val live: RLayer[Has[TransactorLayer], HealthCheck] =
    ZLayer.fromService[TransactorLayer, HealthCheck.Service] { transactor =>
      new HealthCheck.Service {
        override val healthCheck: UIO[DbStatus] =
          sql"""SELECT 1"""
            .query[Int]
            .unique
            .transact(transactor)
            .refineOrDie { case e: Exception =>
              RepositoryError(e)
            }
            .fold(
              _ => DbStatus(false),
              _ => DbStatus(true)
            )

      }
    }
}
