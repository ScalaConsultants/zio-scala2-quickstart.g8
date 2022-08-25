package $package$.domain

import zio.{ IO, ZIO }

trait ItemRepository {

  def add(data: ItemData): IO[RepositoryError, ItemId]

  def delete(id: ItemId): IO[RepositoryError, Int]

  def getAll: IO[RepositoryError, List[Item]]

  def getById(id: ItemId): IO[RepositoryError, Option[Item]]

  def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]]

  def update(id: ItemId, data: ItemData): IO[RepositoryError, Option[Unit]]

}

object ItemRepository {

  def add(data: ItemData): ZIO[ItemRepository, RepositoryError, ItemId] =
    ZIO.environmentWithZIO(_.get.add(data))

  def delete(id: ItemId): ZIO[ItemRepository, RepositoryError, Int] =
    ZIO.environmentWithZIO(_.get.delete(id))

  def getAll: ZIO[ItemRepository, RepositoryError, List[Item]] =
    ZIO.environmentWithZIO(_.get.getAll)

  def getById(id: ItemId): ZIO[ItemRepository, RepositoryError, Option[Item]] =
    ZIO.environmentWithZIO(_.get.getById(id))

  def getByIds(ids: Set[ItemId]): ZIO[ItemRepository, RepositoryError, List[Item]] =
    ZIO.environmentWithZIO(_.get.getByIds(ids))

  def update(id: ItemId, data: ItemData): ZIO[ItemRepository, RepositoryError, Option[Unit]] =
    ZIO.environmentWithZIO(_.get.update(id, data))

}
