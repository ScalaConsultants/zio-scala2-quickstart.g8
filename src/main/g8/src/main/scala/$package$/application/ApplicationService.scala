package $package$.application

import zio.ZIO

import $package$.domain._

object ApplicationService {

  def addItem(name: String, price: BigDecimal): ZIO[ItemRepository, DomainError, ItemId] =
    ZIO.serviceWithZIO[ItemRepository](_.add(ItemData(name, price)))

  def deleteItem(id: ItemId): ZIO[ItemRepository, DomainError, Int] =
    ZIO.serviceWithZIO[ItemRepository](_.delete(id))

  def getAllItems(): ZIO[ItemRepository, DomainError, List[Item]] =
    ZIO.serviceWithZIO[ItemRepository](_.getAll)

  def getItemById(id: ItemId): ZIO[ItemRepository, DomainError, Option[Item]] =
    ZIO.serviceWithZIO[ItemRepository](_.getById(id))

  def updateItem(
    id: ItemId,
    name: String,
    price: BigDecimal
  ): ZIO[ItemRepository, DomainError, Option[Item]] =
    for {
      repo         <- ZIO.service[ItemRepository]
      data         <- ZIO.succeed(ItemData(name, price))
      maybeUpdated <- repo.update(id, data)
    } yield maybeUpdated.map(_ => Item.withData(id, data))

  def partialUpdateItem(
    id: ItemId,
    name: Option[String],
    price: Option[BigDecimal]
  ): ZIO[ItemRepository, DomainError, Option[Item]] = {

    val result = for {
      repo        <- ZIO.service[ItemRepository]
      currentItem <- repo.getById(id).some
      data         = ItemData(name.getOrElse(currentItem.name), price.getOrElse(currentItem.price))
      _           <- repo.update(id, data).some
    } yield Item.withData(id, data)

    result.unsome
  }

}
