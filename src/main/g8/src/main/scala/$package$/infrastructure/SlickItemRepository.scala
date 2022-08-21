package $package$.infrastructure

import $package$.domain._
import $package$.infrastructure.tables.ItemsTable
import $package$.infrastructure.Profile
import slick.interop.zio.DatabaseProvider
import slick.interop.zio.syntax._
import zio.logging._
import zio._

object SlickItemRepository {

  val live: RLayer[Has[DatabaseProvider] with Logging, Has[ItemRepository]] =
    ZLayer.fromServicesM[DatabaseProvider, Logger[String], Any, Nothing, ItemRepository] { case (db, log) => 
      db.profile.map { jdbcProfile => 
        new ItemRepository with ItemsTable with Profile {
          override lazy val profile = jdbcProfile
          import profile.api._

          val items = table

          def add(data: ItemData): IO[RepositoryError, ItemId] = {
            val insert = (items returning items.map(_.id)) += Item.withData(ItemId(0), data)
  
            log.info(s"Adding item \$data") *>
            ZIO
              .fromDBIO(insert)
              .provideLayer(ZLayer.succeed(db))
              .refineOrDie {
                case e: Exception => RepositoryError(e)
              }
          }

          def delete(id: ItemId): IO[RepositoryError, Int] = {
            val deleteRequest = items.filter(_.id === id).delete
            Console.println("deleted" + deleteRequest.toString)
            ZIO.fromDBIO(deleteRequest).provideLayer(ZLayer.succeed(db)).refineOrDie {
              case e: Exception => RepositoryError(e)
            }
          }

          val getAll: IO[RepositoryError, List[Item]] =
            ZIO.fromDBIO(items.result).provideLayer(ZLayer.succeed(db)).map(_.toList).refineOrDie {
              case e: Exception => RepositoryError(e)
            }

          def getById(id: ItemId): IO[RepositoryError, Option[Item]] = {
            val query = items.filter(_.id === id).result
  
            ZIO.fromDBIO(query).provideLayer(ZLayer.succeed(db)).map(_.headOption).refineOrDie {
              case e: Exception => RepositoryError(e)
            }
          }

          def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]] = {
            val query = items.filter(_.id inSet ids).result
  
            ZIO.fromDBIO(query).provideLayer(ZLayer.succeed(db)).map(_.toList).refineOrDie {
              case e: Exception => RepositoryError(e)
            }
          }

          def update(id: ItemId, data: ItemData): IO[RepositoryError, Option[Unit]] = {
            val q      = items.filter(_.id === id).map(item => (item.name, item.price))
            val update = q.update((data.name, data.price))
  
            val foundF = (n: Int) => if (n > 0) Some(()) else None
  
            log.info(s"Updating item \${id.value} to \$data") *>
            ZIO.fromDBIO(update).provideLayer(ZLayer.succeed(db)).map(foundF).refineOrDie {
              case e: Exception => RepositoryError(e)
            }
          }
        }
      }
    }
}
