package $package$.application

import zio.{ Has, IO, URLayer, ZIO, ZLayer }
$if(add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
import zio.stream.{ Stream, ZStream }
$endif$
import $package$.domain._

trait ApplicationService {

  def addItem(name: String, price: BigDecimal): IO[DomainError, ItemId]

  $if(add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
  def deletedEvents: Stream[Nothing, ItemId]
    
  $endif$
  def deleteItem(itemId: ItemId): IO[DomainError, Int]
  
  def getItem(itemId: ItemId): IO[DomainError, Option[Item]]
  
  val getItems: IO[DomainError, List[Item]]
  
  def partialUpdateItem(
    itemId: ItemId,
    name: Option[String],
    price: Option[BigDecimal]
  ): IO[DomainError, Option[Unit]]
  
  def updateItem(
    itemId: ItemId,
    name: String,
    price: BigDecimal
  ): IO[DomainError, Option[Unit]]
}

object ApplicationService {

  $if(add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
  val live: URLayer[Has[ItemRepository] with Has[Subscriber], Has[ApplicationService]] = ZLayer.fromServices[ItemRepository, Subscriber, ApplicationService] { case (repo, sbscr) =>
  $else$
  val live: URLayer[Has[ItemRepository], Has[ApplicationService]] = ZLayer.fromService { repo =>
  $endif$
    new ApplicationService {
      def addItem(name: String, price: BigDecimal): IO[DomainError, ItemId] = repo.add(ItemData(name, price))
  
      $if(add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
      def deletedEvents: Stream[Nothing, ItemId] = sbscr.showDeleteEvents
    
      def deleteItem(itemId: ItemId): IO[DomainError, Int] = 
        for {
          out <- repo.delete(itemId)
          _   <- sbscr.publishDeleteEvents(itemId)
        } yield out

      $else$
      def deleteItem(itemId: ItemId): IO[DomainError, Int] = repo.delete(itemId)
      $endif$

      def getItem(itemId: ItemId): IO[DomainError, Option[Item]] = repo.getById(itemId)

      val getItems: IO[DomainError, List[Item]] = repo.getAll
  
      def partialUpdateItem(
        itemId: ItemId,
        name: Option[String],
        price: Option[BigDecimal]
      ): IO[DomainError, Option[Unit]] = 
        (for {
          item <- repo.getById(itemId).some
          _ <- repo.update(itemId, ItemData(name.getOrElse(item.name), price.getOrElse(item.price)))
                .mapError(Some(_))
        } yield ()).optional
  
      def updateItem(
        itemId: ItemId,
        name: String,
        price: BigDecimal
      ): IO[DomainError, Option[Unit]] = repo.update(itemId, ItemData(name, price))
    }
  }

  def addItem(name: String, price: BigDecimal): ZIO[Has[ApplicationService], DomainError, ItemId] =
    ZIO.accessM(_.get.addItem(name, price))

  $if(add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
  def deletedEvents: ZStream[Has[ApplicationService], Nothing, ItemId] =
    ZStream.accessStream(_.get.deletedEvents)

  $endif$
  def deleteItem(itemId: ItemId): ZIO[Has[ApplicationService], DomainError, Int] =
    ZIO.accessM(_.get.deleteItem(itemId))

  def getItem(itemId: ItemId): ZIO[Has[ApplicationService], DomainError, Option[Item]] =
    ZIO.accessM(_.get.getItem(itemId))

  val getItems: ZIO[Has[ApplicationService], DomainError, List[Item]] =
    ZIO.accessM(_.get.getItems)

  def partialUpdateItem(
    itemId: ItemId,
    name: Option[String],
    price: Option[BigDecimal]
  ): ZIO[Has[ApplicationService], DomainError, Option[Unit]] =
    ZIO.accessM(_.get.partialUpdateItem(itemId, name, price))

  def updateItem(
    itemId: ItemId,
    name: String,
    price: BigDecimal
  ): ZIO[Has[ApplicationService], DomainError, Option[Unit]] =
    ZIO.accessM(_.get.updateItem(itemId, name, price))

}
