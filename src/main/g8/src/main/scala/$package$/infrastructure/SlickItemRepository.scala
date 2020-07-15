package $package$.infrastructure

import $package$.domain._
import $package$.infrastructure.EntityIdMappers._
import $package$.infrastructure.tables.ItemsTable
import slick.interop.zio.DatabaseProvider
import slick.interop.zio.syntax._
import slick.jdbc.H2Profile.api._
import zio.logging._
import zio.stream.ZStream
import zio.{ IO, Queue, Ref, UIO, ZIO, ZLayer }

final class SlickItemRepository(env: DatabaseProvider with Logging, deletedEventsSubscribers: Ref[List[Queue[ItemId]]])
    extends ItemRepository.Service {
  val items = ItemsTable.table

  def add(data: ItemData): IO[RepositoryError, ItemId] = {
    val insert = (items returning items.map(_.id)) += Item.withData(ItemId(0), data)

    log.info(s"Adding item \$data") *>
    ZIO
      .fromDBIO(insert)
      .refineOrDie {
        case e: Exception => RepositoryError(e)
      }

  }.provide(env)

  def delete(id: ItemId): IO[RepositoryError, Unit] = {
    val delete = items.filter(_.id === id).delete

    log.info(s"Deleting item \${id.value}") *>
    ZIO
      .fromDBIO(delete)
      $if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
      .flatMap(deletedCount => ZIO.when(deletedCount > 0)(publishDeletedEvents(id)))
      $else$
      .unit
      $endif$
      .refineOrDie {
        case e: Exception => RepositoryError(e)
      }
  }.provide(env)

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

  $if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
  def deletedEvents: ZStream[Any, Nothing, ItemId] = ZStream.unwrap {
    for {
      queue <- Queue.unbounded[ItemId]
      _     <- deletedEventsSubscribers.update(queue :: _)
    } yield ZStream.fromQueue(queue)
  }

  private def publishDeletedEvents(deletedItemId: ItemId) =
    log.info(s"Publishing delete event for item \${deletedItemId.value}") *>
      deletedEventsSubscribers.get.flatMap[Any, Nothing, List[Boolean]](subs =>
        // send item to all subscribers
        UIO.foreach(subs)(queue =>
          queue
            .offer(deletedItemId)
            .onInterrupt(
              // if queue was shutdown, remove from subscribers
              deletedEventsSubscribers.update(_.filterNot(_ == queue))
            )
        )
      )
  $endif$
}

object SlickItemRepository {

  val live: ZLayer[DatabaseProvider with Logging, Throwable, ItemRepository] =
    ZLayer.fromFunctionM { env =>
      val initialize = ZIO.fromDBIO(ItemsTable.table.schema.createIfNotExists) *>
        Ref.make(List.empty[Queue[ItemId]])

      initialize.provide(env).map(new SlickItemRepository(env, _))
    }
}
