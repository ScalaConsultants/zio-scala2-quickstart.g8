package $package$.application

import zio.{ IO, URLayer, ZIO, ZLayer }

import $package$.domain._

trait ApplicationService {

  def addItem(name: String, price: BigDecimal): IO[DomainError, ItemId]

  def deleteItem(itemId: ItemId): IO[DomainError, Int]

  def getItem(itemId: ItemId): IO[DomainError, Option[Item]]

  val getItems: IO[DomainError, List[Item]]

  def partialUpdateItem(itemId: ItemId, name: Option[String], price: Option[BigDecimal]): IO[DomainError, Option[Item]]

  def updateItem(itemId: ItemId, name: String, price: BigDecimal): IO[DomainError, Option[Item]]
}

object ApplicationService {

  val live: URLayer[ItemRepository, ApplicationService] = ZLayer {
    for {
      repo <- ZIO.service[ItemRepository]
    } yield new ApplicationService {
      override def addItem(name: String, price: BigDecimal): IO[DomainError, ItemId] =
        repo.add(ItemData(name, price))

      override def deleteItem(itemId: ItemId): IO[DomainError, Int] =
        repo.delete(itemId)

      override def getItem(itemId: ItemId): IO[DomainError, Option[Item]] =
        repo.getById(itemId)

      override val getItems: IO[DomainError, List[Item]] =
        repo.getAll

      override def partialUpdateItem(
        itemId: ItemId,
        name: Option[String],
        price: Option[BigDecimal]
      ): IO[DomainError, Option[Item]] = {

        val result = for {
          item <- repo.getById(itemId).some
          data  = ItemData(name.getOrElse(item.name), price.getOrElse(item.price))
          _    <- repo
                    .update(itemId, data)
                    .mapError(Some(_))
        } yield Item.withData(itemId, data)

        result.unsome
      }

      def updateItem(itemId: ItemId, name: String, price: BigDecimal): IO[DomainError, Option[Item]] =
        for {
          data         <- ZIO.succeed(ItemData(name, price))
          maybeUpdated <- repo.update(itemId, data)
          maybeItem     = maybeUpdated.map(_ => Item.withData(itemId, data))
        } yield maybeItem
    }
  }

  def addItem(name: String, price: BigDecimal): ZIO[ApplicationService, DomainError, ItemId] =
    ZIO.environmentWithZIO(_.get.addItem(name, price))

  def deleteItem(itemId: ItemId): ZIO[ApplicationService, DomainError, Int] =
    ZIO.environmentWithZIO(_.get.deleteItem(itemId))

  def getItem(itemId: ItemId): ZIO[ApplicationService, DomainError, Option[Item]] =
    ZIO.environmentWithZIO(_.get.getItem(itemId))

  val getItems: ZIO[ApplicationService, DomainError, List[Item]] =
    ZIO.environmentWithZIO(_.get.getItems)

  def partialUpdateItem(
    itemId: ItemId,
    name: Option[String],
    price: Option[BigDecimal]
  ): ZIO[ApplicationService, DomainError, Option[Item]] =
    ZIO.environmentWithZIO(_.get.partialUpdateItem(itemId, name, price))

  def updateItem(itemId: ItemId, name: String, price: BigDecimal): ZIO[ApplicationService, DomainError, Option[Item]] =
    ZIO.environmentWithZIO(_.get.updateItem(itemId, name, price))

}
