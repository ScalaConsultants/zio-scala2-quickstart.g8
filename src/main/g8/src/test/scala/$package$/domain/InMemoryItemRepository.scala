package $package$.domain

import zio._

final class InMemoryItemRepository(storage: Ref[List[Item]]) extends ItemRepository.Service {
  def add(name: String, price: BigDecimal): IO[RepositoryError, ItemId] =
    storage.modify{ items =>
        val nextId = ItemId(
          if(items.isEmpty) 0L
          else items.map(_.id.fold(0L)(_.value)).max + 1L
        )

        nextId -> (Item(Some(nextId), name, price) :: items)
    }

  def delete(id: ItemId): IO[RepositoryError, Unit] =
    storage.modify{ items =>
        () -> items.filterNot(_.id.contains(id))
    }

  val getAll: IO[RepositoryError, List[Item]] =
    storage.get

  def getById(id: ItemId): IO[RepositoryError, Option[Item]] =
    getAll.map(_.find(_.id.contains(id)))

  def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]] =
    getAll.map(_.filter(_.id.exists(ids)))

  def update(id: ItemId, name: String, price: BigDecimal): IO[RepositoryError, Option[Unit]] =
    storage.modify{ items =>
      val newItems = items.map{
        case i if i.id.contains(id) => i.copy(name = name, price = price)
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
