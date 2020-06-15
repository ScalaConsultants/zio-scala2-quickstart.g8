package $package$.domain

import zio._
import zio.stream.ZStream

final class InMemoryItemRepository(storage: Ref[List[Item]], deletedEventsSubscribers: Ref[List[Queue[ItemId]]])
    extends ItemRepository.Service {
  def add(data: ItemData): IO[RepositoryError, ItemId] =
    storage.modify { items =>
      val nextId = ItemId(
        if (items.isEmpty) 0L
        else items.map(_.id.value).max + 1L
      )

      nextId -> (Item.withData(nextId, data) :: items)
    }

  def delete(id: ItemId): IO[RepositoryError, Unit] =
    storage
      .modify(items => () -> items.filterNot(_.id == id))
      .flatMap(_ => publishDeletedEvents(id).unit)

  val getAll: IO[RepositoryError, List[Item]] =
    storage.get

  def getById(id: ItemId): IO[RepositoryError, Option[Item]] =
    getAll.map(_.find(_.id == id))

  def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]] =
    getAll.map(_.filter(i => ids(i.id)))

  def update(id: ItemId, data: ItemData): IO[RepositoryError, Option[Unit]] =
    storage.modify { items =>
      val newItems = items.map {
        case i if i.id == id => i.copy(name = data.name, price = data.price)
        case i               => i
      }
      val updated = if (newItems == items) None else Some(())
      updated -> newItems
    }

  def getByName(name: String): IO[RepositoryError, List[Item]] =
    getAll.map(_.filter(_.name == name))

  def getCheaperThan(price: BigDecimal): IO[RepositoryError, List[Item]] =
    getAll.map(_.filter(_.price < price))

  def deletedEvents: ZStream[Any, Nothing, ItemId] = ZStream.unwrap {
    for {
      queue <- Queue.unbounded[ItemId]
      _     <- deletedEventsSubscribers.update(queue :: _)
    } yield ZStream.fromQueue(queue)
  }

  private def publishDeletedEvents(deletedItemId: ItemId) =
    deletedEventsSubscribers.get.flatMap(subs =>
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
}

object InMemoryItemRepository {

  val test: Layer[Nothing, ItemRepository] = ZLayer.fromEffect(for {
    storage <- Ref.make(List.empty[Item])
    deleted <- Ref.make(List.empty[Queue[ItemId]])
  } yield new InMemoryItemRepository(storage, deleted))
}
