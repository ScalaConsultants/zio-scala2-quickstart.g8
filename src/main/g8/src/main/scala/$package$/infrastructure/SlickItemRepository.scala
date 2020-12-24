package $package$.infrastructure

import $package$.domain._
import $package$.infrastructure.tables.ItemsTable
import $package$.infrastructure.Profile
import slick.interop.zio.DatabaseProvider
import slick.interop.zio.syntax._
import slick.jdbc.PostgresProfile
import zio.logging._
import zio._

final class SlickItemRepository(env: DatabaseProvider with Logging) extends ItemRepository.Service
    with ItemsTable  with Profile {
  type P = PostgresProfile
  override lazy val profile = PostgresProfile
  import profile.api._

  val items = table

  def add(data: ItemData): IO[RepositoryError, ItemId] = {
    val insert = (items returning items.map(_.id)) += Item.withData(ItemId(0), data)

    log.info(s"Adding item \$data") *>
    ZIO
      .fromDBIO(insert)
      .refineOrDie {
        case e: Exception => RepositoryError(e)
      }

  }.provide(env)

  def delete(id: ItemId): ZIO[Any, RepositoryError, Int] = {
    val deleteRequest = items.filter(_.id === id).delete
    Console.println("deleted" + deleteRequest.toString)
    ZIO.fromDBIO(deleteRequest).provide(env).refineOrDie {
     case e: Exception => RepositoryError(e)
    }
  }

  val getAll: IO[RepositoryError, List[Item]] =
    ZIO.fromDBIO(items.result).provide(env).map(_.toList).refineOrDie {
      case e: Exception => RepositoryError(e)
    }

  def getById(id: ItemId): IO[RepositoryError, Option[Item]] = {
    val query = items.filter(_.id === id).result

    ZIO.fromDBIO(query).provide(env).map(_.headOption).refineOrDie {
      case e: Exception => RepositoryError(e)
    }
  }

  def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]] = {
    val query = items.filter(_.id inSet ids).result

    ZIO.fromDBIO(query).provide(env).map(_.toList).refineOrDie {
      case e: Exception => RepositoryError(e)
    }
  }

  $if(add_caliban_endpoint.truthy)$
  def getByName(name: String): IO[RepositoryError, List[Item]] = {
    val query = items.filter(_.name === name).result

    ZIO.fromDBIO(query).provide(env).map(_.toList).refineOrDie {
      case e: Exception => RepositoryError(e)
    }
  }

  def getCheaperThan(price: BigDecimal): IO[RepositoryError, List[Item]] = {
    val query = items.filter(_.price < price).result

    ZIO.fromDBIO(query).provide(env).map(_.toList).refineOrDie {
      case e: Exception => RepositoryError(e)
    }
  }
  $endif$

  def update(id: ItemId, data: ItemData): IO[RepositoryError, Option[Unit]] = {
    val q      = items.filter(_.id === id).map(item => (item.name, item.price))
    val update = q.update((data.name, data.price))

    val foundF = (n: Int) => if (n > 0) Some(()) else None

    log.info(s"Updating item \${id.value} to \$data") *>
    ZIO.fromDBIO(update).map(foundF).refineOrDie {
      case e: Exception => RepositoryError(e)
    }
  }.provide(env)

  val healthCheck: UIO[Boolean] =
  ZIO
  .fromDBIO(items.result)
  .provide(env)
  .fold(
  _ => false,
  _ => true
  )


}

object SlickItemRepository {

  val live: RLayer[DatabaseProvider with Logging, ItemRepository] =
    ZLayer.fromFunction(env => new SlickItemRepository(env))
}
