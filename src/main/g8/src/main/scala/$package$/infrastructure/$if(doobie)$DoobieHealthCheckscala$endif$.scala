package $package$.infrastructure

import com.example.domain.{ DbStatus, HealthCheck, RepositoryError, TransactorLayer }
import doobie.implicits._
import zio._
import zio.interop.catz.taskConcurrentInstance

object DoobieHealthCheck {
  $if(slick)$SlickHealthCheck.scala$endif$
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
