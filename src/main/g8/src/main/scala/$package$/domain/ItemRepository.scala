package $package$.domain

import zio.{IO}


object ItemRepository {

  trait Service {

    def add(data: ItemData): IO[RepositoryError, ItemId]

    def delete(id: ItemId): IO[RepositoryError, Int]

    val getAll: IO[RepositoryError, List[Item]]

    def getById(id: ItemId): IO[RepositoryError, Option[Item]]

    $if(add_caliban_endpoint.truthy)$
    def getByName(name: String): IO[RepositoryError, List[Item]]

    def getCheaperThan(price: BigDecimal): IO[RepositoryError, List[Item]]
    $endif$

    def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]]

    def update(id: ItemId, data: ItemData): IO[RepositoryError, Option[Unit]]

  }
}
