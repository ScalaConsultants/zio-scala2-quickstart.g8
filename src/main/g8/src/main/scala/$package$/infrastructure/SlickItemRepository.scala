package $package$.infrastructure

import $package$.domain._
import $package$.infrastructure.EntityIdMappers._
import $package$.infrastructure.tables.ItemsTable
import $package$.interop.slick.dbio._
import $package$.interop.slick.DatabaseProvider
import slick.jdbc.H2Profile.api._
import zio.{ IO, ZIO, ZLayer }

object SlickItemRepository {

  val live: ZLayer[DatabaseProvider, Throwable, ItemRepository] =
    ZLayer.fromFunctionM { dbProvider: DatabaseProvider =>
      val items      = ItemsTable.table
      val initialize = ZIO.fromDBIO(items.schema.createIfNotExists)

      val service: ItemRepository.Service = new ItemRepository.Service {

        def add(name: String, price: BigDecimal): IO[RepositoryError, ItemId] = {
          val insert = (items returning items.map(_.id)) += Item(ItemId(0), name, price)

          ZIO.fromDBIO(insert).provide(dbProvider).refineOrDie {
            case e: Exception => RepositoryError(e)
          }
        }

        def delete(id: ItemId): IO[RepositoryError, Unit] = {
          val delete = items.filter(_.id === id).delete

          ZIO.fromDBIO(delete).provide(dbProvider).unit.refineOrDie {
            case e: Exception => RepositoryError(e)
          }
        }

        val getAll: IO[RepositoryError, List[Item]] =
          ZIO.fromDBIO(items.result).provide(dbProvider).map(_.toList).refineOrDie {
            case e: Exception => RepositoryError(e)
          }

        def getById(id: ItemId): IO[RepositoryError, Option[Item]] = {
          val query = items.filter(_.id === id).result

          ZIO.fromDBIO(query).provide(dbProvider).map(_.headOption).refineOrDie {
            case e: Exception => RepositoryError(e)
          }
        }

        def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]] = {
          val query = items.filter(_.id inSet ids).result

          ZIO.fromDBIO(query).provide(dbProvider).map(_.toList).refineOrDie {
            case e: Exception => RepositoryError(e)
          }
        }

        def getByName(name: String): IO[RepositoryError, Option[Item]] = {
          val query = items.filter(_.name === name).result

          ZIO.fromDBIO(query).provide(dbProvider).map(_.headOption).refineOrDie {
            case e: Exception => RepositoryError(e)
          }
        }

        def update(id: ItemId, name: String, price: BigDecimal): IO[RepositoryError, Option[Unit]] = {
          val q      = items.filter(_.id === id).map(item => (item.name, item.price))
          val update = q.update((name, price))

          val foundF = (n: Int) => if (n > 0) Some(()) else None

          ZIO.fromDBIO(update).provide(dbProvider).map(foundF).refineOrDie {
            case e: Exception => RepositoryError(e)
          }
        }
      }

      initialize.provide(dbProvider).as(service)
    }
}
