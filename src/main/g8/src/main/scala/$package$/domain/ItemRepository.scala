package $package$.domain

import zio.{ Has, IO, ZIO }

trait ItemRepository {

  def add(data: ItemData): IO[RepositoryError, ItemId]

  def delete(id: ItemId): IO[RepositoryError, Int]

  def getAll: IO[RepositoryError, List[Item]]

  def getById(id: ItemId): IO[RepositoryError, Option[Item]]

  def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]]

  def update(id: ItemId, data: ItemData): IO[RepositoryError, Option[Unit]]

}

object ItemRepository {

  def add(data: ItemData): ZIO[Has[ItemRepository], RepositoryError, ItemId] = ZIO.accessM(_.get.add(data))

  def delete(id: ItemId): ZIO[Has[ItemRepository], RepositoryError, Int] = ZIO.accessM(_.get.delete(id))

  def getAll: ZIO[Has[ItemRepository], RepositoryError, List[Item]] = ZIO.accessM(_.get.getAll)

  def getById(id: ItemId): ZIO[Has[ItemRepository], RepositoryError, Option[Item]] = ZIO.accessM(_.get.getById(id))

  def getByIds(ids: Set[ItemId]): ZIO[Has[ItemRepository], RepositoryError, List[Item]] = ZIO.accessM(_.get.getByIds(ids))

  def update(id: ItemId, data: ItemData): ZIO[Has[ItemRepository], RepositoryError, Option[Unit]] = ZIO.accessM(_.get.update(id, data))

}
