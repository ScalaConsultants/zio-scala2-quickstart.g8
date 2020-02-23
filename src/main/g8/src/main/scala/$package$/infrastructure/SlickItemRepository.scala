package $package$.infrastructure

import $package$.domain._
import $package$.infrastructure.EntityIdMappers._
import $package$.infrastructure.tables.ItemsTable
import $package$.interop.slick.dbio._
import $package$.interop.slick.DatabaseProvider
import slick.jdbc.H2Profile.api._
import zio.{ IO, ZIO }

trait SlickItemRepository extends ItemRepository with DatabaseProvider { self =>

  val items = TableQuery[ItemsTable.Items]

  val itemRepository = new ItemRepository.Service {

    def add(name: String, price: BigDecimal): IO[RepositoryFailure, ItemId] = {
      val insert = (items returning items.map(_.id)) += Item(None, name, price)

      ZIO.fromDBIO(insert).provide(self).refineOrDie {
        case e: Exception => new RepositoryFailure(e)
      }
    }

    def delete(id: ItemId): IO[RepositoryFailure, Unit] = {
      val delete = items.filter(_.id === id).delete

      ZIO.fromDBIO(delete).provide(self).map(_ => ()).refineOrDie {
        case e: Exception => new RepositoryFailure(e)
      }
    }

    val getAll: IO[RepositoryFailure, List[Item]] = 
      ZIO.fromDBIO(items.result).provide(self).map(_.toList).refineOrDie {
        case e: RepositoryFailure => e
      }

    def getById(id: ItemId): IO[RepositoryFailure, Option[Item]] = {
      val query = items.filter(_.id === id).result

      ZIO.fromDBIO(query).provide(self).map(_.headOption).refineOrDie {
        case e: Exception => new RepositoryFailure(e)
      }      
    }

    def getByIds(ids: Set[ItemId]): IO[RepositoryFailure, List[Item]] = {
      val query = items.filter(_.id inSet ids).result

      ZIO.fromDBIO(query).provide(self).map(_.toList).refineOrDie {
        case e: Exception => new RepositoryFailure(e)
      }
    }

    def getByName(name: String): IO[RepositoryFailure, Option[Item]] = {
      val query = items.filter(_.name === name).result

      ZIO.fromDBIO(query).provide(self).map(_.headOption).refineOrDie {
        case e: Exception => new RepositoryFailure(e)
      }      
    }

    def update(id: ItemId, name: String, price: BigDecimal): IO[RepositoryFailure, Option[Unit]] = {
      val q = items.filter(_.id === id).map(item => (item.name, item.price))
      val update = q.update((name, price))

      val foundF = (n: Int) => if (n > 0) Some(()) else None

      ZIO.fromDBIO(update).provide(self).map(foundF).refineOrDie {
        case e: Exception => new RepositoryFailure(e)
      }
    }
  }

}
