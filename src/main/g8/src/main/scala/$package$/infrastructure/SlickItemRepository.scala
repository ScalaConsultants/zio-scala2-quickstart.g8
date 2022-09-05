package $package$.infrastructure

import slick.jdbc.JdbcProfile
import slick.interop.zio.syntax._
import slick.interop.zio.DatabaseProvider

import zio._

import $package$.domain._
import $package$.infrastructure.tables.ItemsTable

class SlickItemRepository(databaseProvider: DatabaseProvider, jdbcProfile: JdbcProfile)
    extends ItemRepository
    with ItemsTable
    with Profile {

  override val profile = jdbcProfile

  override def add(data: ItemData): IO[RepositoryError, ItemId] = {
    import profile.api._

    val insert = (table returning table.map(_.id)) += Item.withData(ItemId(0), data)

    ZIO.logInfo(s"Adding item \$data") *>
    ZIO
      .fromDBIO(insert)
      .provideLayer(ZLayer.succeed(databaseProvider))
      .refineOrDie { case e: Exception => RepositoryError(e) }
  }

  override def delete(id: ItemId): IO[RepositoryError, Int] = {
    import profile.api._

    val deleteRequest = table.filter(_.id === id).delete

    ZIO.logInfo("deleted" + deleteRequest.toString) *>
    ZIO
      .fromDBIO(deleteRequest)
      .provideLayer(ZLayer.succeed(databaseProvider))
      .refineOrDie { case e: Exception => RepositoryError(e) }
  }

  override val getAll: IO[RepositoryError, List[Item]] = {
    import profile.api._

    ZIO
      .fromDBIO(table.result)
      .provideLayer(ZLayer.succeed(databaseProvider))
      .map(_.toList)
      .refineOrDie { case e: Exception => RepositoryError(e) }
  }

  override def getById(id: ItemId): IO[RepositoryError, Option[Item]] = {
    import profile.api._

    val query = table.filter(_.id === id).result

    ZIO
      .fromDBIO(query)
      .provideLayer(ZLayer.succeed(databaseProvider))
      .map(_.headOption)
      .refineOrDie { case e: Exception => RepositoryError(e) }
  }

  override def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]] = {
    import profile.api._

    val query = table.filter(_.id inSet ids).result

    ZIO
      .fromDBIO(query)
      .provideLayer(ZLayer.succeed(databaseProvider))
      .map(_.toList)
      .refineOrDie { case e: Exception => RepositoryError(e) }
  }

  override def update(id: ItemId, data: ItemData): IO[RepositoryError, Option[Unit]] = {
    import profile.api._

    val update = table
      .filter(_.id === id)
      .map(item => (item.name, item.price))
      .update((data.name, data.price))

    ZIO.logInfo(s"Updating item \${id.value} to \$data") *>
    ZIO
      .fromDBIO(update)
      .provideLayer(ZLayer.succeed(databaseProvider))
      .map(n => if (n > 0) Some(()) else None)
      .refineOrDie { case e: Exception => RepositoryError(e) }
  }
}

object SlickItemRepository {

  val live: RLayer[DatabaseProvider, ItemRepository] = ZLayer {
    for {
      databaseProvider <- ZIO.service[DatabaseProvider]
      jdbcProfile      <- databaseProvider.profile
    } yield new SlickItemRepository(databaseProvider, jdbcProfile)
  }
}
