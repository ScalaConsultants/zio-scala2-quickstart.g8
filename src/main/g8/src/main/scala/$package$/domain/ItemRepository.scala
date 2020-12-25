package $package$.domain

import zio.{ IO, ZIO }

object ItemRepository {

  trait Service {

    def add(data: ItemData): IO[RepositoryError, ItemId]

    def delete(id: ItemId): IO[RepositoryError, Int]

    def getAll: IO[RepositoryError, List[Item]]

    def getById(id: ItemId): IO[RepositoryError, Option[Item]]

    def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]]

    $if(add_caliban_endpoint.truthy)$
    def getByName(name: String): IO[RepositoryError, List[Item]]

    def getCheaperThan(price: BigDecimal): IO[RepositoryError, List[Item]]
    $endif$

    def update(id: ItemId, data: ItemData): IO[RepositoryError, Option[Unit]]

  }

  def add(data: ItemData): ZIO[ItemRepository, RepositoryError, ItemId] = ZIO.accessM(_.get.add(data))

  def delete(id: ItemId): ZIO[ItemRepository, RepositoryError, Int] = ZIO.accessM(_.get.delete(id))

  def getAll: ZIO[ItemRepository, RepositoryError, List[Item]] = ZIO.accessM(_.get.getAll)

  def getById(id: ItemId): ZIO[ItemRepository, RepositoryError, Option[Item]] = ZIO.accessM(_.get.getById(id))

  def getByIds(ids: Set[ItemId]): ZIO[ItemRepository, RepositoryError, List[Item]] = ZIO.accessM(_.get.getByIds(ids))

  $if(add_caliban_endpoint.truthy)$
  def getByName(name: String): ZIO[ItemRepository, RepositoryError, List[Item]] = ZIO.accessM(_.get.getByName(name))

  def getCheaperThan(price: BigDecimal): ZIO[ItemRepository, RepositoryError, List[Item]] = ZIO.accessM(_.get.getCheaperThan(price))
  $endif$

  def update(id: ItemId, data: ItemData): ZIO[ItemRepository, RepositoryError, Option[Unit]] = ZIO.accessM(_.get.update(id, data))

}
