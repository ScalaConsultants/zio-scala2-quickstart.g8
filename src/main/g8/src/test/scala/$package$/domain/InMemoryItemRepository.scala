package $package$.domain

import zio._

final class InMemoryItemRepository(storage: Ref[List[Item]]) extends ItemRepository.Service {
  def add(data: ItemData): IO[RepositoryError, ItemId] =
    storage.modify{ items =>
        val nextId = ItemId(
          if(items.isEmpty) 0L
          else items.map(_.id.value).max + 1L
        )

        nextId -> (Item.withData(nextId, data) :: items)
    }

  def delete(id: ItemId): IO[RepositoryError, Unit] =
    storage.modify{ items =>
        () -> items.filterNot(_.id == id)
    }

  val getAll: IO[RepositoryError, List[Item]] =
    storage.get

  def getById(id: ItemId): IO[RepositoryError, Option[Item]] =
    getAll.map(_.find(_.id == id))

  def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]] =
    getAll.map(_.filter(i => ids(i.id)))

  def update(id: ItemId, data: ItemData): IO[RepositoryError, Option[Unit]] =
    storage.modify{ items =>
      val newItems = items.map{
        case i if i.id == id => i.copy(name = data.name, price = data.price)
        case i => i
      }
      val updated = if(newItems == items) None else Some(())
      updated -> newItems
    }
}

object InMemoryItemRepository{

  val test: Layer[Nothing, ItemRepository] =
      ZLayer.fromEffect(Ref.make(List.empty[Item]).map(new InMemoryItemRepository(_)))
}
