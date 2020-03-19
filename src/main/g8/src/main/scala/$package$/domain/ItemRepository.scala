package $package$.domain

import zio.{ IO, ZIO }

object ItemRepository {

  trait Service {

    def add(name: String, price: BigDecimal): IO[RepositoryError, ItemId]

    def delete(id: ItemId): IO[RepositoryError, Unit]

    val getAll: IO[RepositoryError, List[Item]]

    def getById(id: ItemId): IO[RepositoryError, Option[Item]]

    def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]]

    def update(id: ItemId, name: String, price: BigDecimal): IO[RepositoryError, Option[Unit]]
  }

  val getAll: ZIO[ItemRepository, RepositoryError, List[Item]] = ZIO.accessM[ItemRepository](_.get.getAll)
  def add(name: String, price: BigDecimal): ZIO[ItemRepository, RepositoryError, ItemId] =
    ZIO.accessM[ItemRepository](_.get.add(name, price))
}
