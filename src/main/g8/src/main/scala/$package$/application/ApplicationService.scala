package $package$.application

import zio.ZIO
$if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
import zio.stream.ZStream
$endif$
import $package$.domain._

object ApplicationService {


  $if(add_caliban_endpoint.truthy)$
  def getItemsCheaperThan(price: BigDecimal): ZIO[ItemRepository, DomainError, List[Item]] =
    ZIO.accessM(_.get.getCheaperThan(price))

  def getItemByName(name: String): ZIO[ItemRepository, DomainError, List[Item]] =
    ZIO.accessM(_.get.getByName(name))
  $endif$

  def addItem(name: String, price: BigDecimal): ZIO[ItemRepository, DomainError, ItemId] =
    ZIO.accessM(_.get.add(ItemData(name, price)))

  def getItem(itemId: ItemId): ZIO[ItemRepository, DomainError, Option[Item]] =
    ZIO.accessM(_.get.getById(itemId))

  val getItems: ZIO[ItemRepository, DomainError, List[Item]] =
    ZIO.accessM(_.get.getAll)

  def partialUpdateItem(
    itemId: ItemId,
    name: Option[String],
    price: Option[BigDecimal]
  ): ZIO[ItemRepository, DomainError, Option[Unit]] =
    (for {
      item <- ZIO.accessM[ItemRepository](_.get.getById(itemId)).some
      _ <- ZIO
            .accessM[ItemRepository](
              _.get.update(itemId, ItemData(name.getOrElse(item.name), price.getOrElse(item.price)))
            )
            .mapError(Some(_))
    } yield ()).optional

  def updateItem(
    itemId: ItemId,
    name: String,
    price: BigDecimal
  ): ZIO[ItemRepository, DomainError, Option[Unit]] =
    ZIO.accessM(_.get.update(itemId, ItemData(name, price)))

  $if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
  def deleteItem(itemId: ItemId): ZIO[ItemRepository with Subscriber, DomainError, Int] = {
    for {
      out <- ZIO.accessM[ItemRepository](_.get.delete(itemId))
      _   <- ZIO.accessM[Subscriber](_.get.publishDeleteEvents(itemId))
    } yield out
  }

  def deletedEvents: ZStream[Subscriber, Nothing, ItemId] =
    ZStream.accessStream(_.get.showDeleteEvents)

    $else$
  def deleteItem(itemId: ItemId): ZIO[ItemRepository, DomainError, Unit] =
    ZIO.accessM(_.get.delete(itemId))
  $endif$
}
