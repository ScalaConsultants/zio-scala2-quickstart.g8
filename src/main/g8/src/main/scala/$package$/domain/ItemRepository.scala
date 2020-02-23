package $package$.domain

import zio.IO

trait ItemRepository {

  def itemRepository: ItemRepository.Service
  
}

object ItemRepository {

  trait Service {

    def add(name: String, price: BigDecimal): IO[RepositoryFailure, ItemId]

    def delete(id: ItemId): IO[RepositoryFailure, Unit]

    val getAll: IO[RepositoryFailure, List[Item]]

    def getById(id: ItemId): IO[RepositoryFailure, Option[Item]]

    def getByIds(ids: Set[ItemId]): IO[RepositoryFailure, List[Item]]

    def update(id: ItemId, name: String, price: BigDecimal): IO[RepositoryFailure, Option[Unit]]
  }

}
