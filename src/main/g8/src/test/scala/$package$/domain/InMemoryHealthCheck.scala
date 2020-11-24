package $package$.domain

import zio._
import zio.stream.ZStream

final class InMemoryHealthCheck(storage: Ref[List[Item]], deletedEventsSubscribers: Ref[List[Queue[ItemId]]])
    extends HealthCheck.Service {


  override val healthCheck: UIO[Boolean] = UIO.succeed(true)

}

object InMemoryHealthCheck {

  val test: Layer[Nothing, ItemRepository] = ZLayer.fromEffect(for {
    storage <- Ref.make(List.empty[Item])
    deleted <- Ref.make(List.empty[Queue[ItemId]])
  } yield new InMemoryItemRepository(storage, deleted))
}
