package $package$.infrastructure.quill

import io.getquill._
import io.getquill.jdbczio.Quill

import zio._

import $package$.domain._

class QuillItemRepository(quill: Quill.Postgres[Literal]) extends ItemRepository {

  import quill._

  private val itemsSchema = quote {
    querySchema[Item]("items")
  }

  override def add(data: ItemData): IO[RepositoryError, ItemId] = {

    val addQuery = quote {
      itemsSchema
        .insertValue(lift(Item.withData(ItemId(0), data)))
        .returningGenerated(_.id)
    }

    run(addQuery).either.resurrect.refineOrDie { case e: NullPointerException =>
      RepositoryError(e)
    }.flatMap {
      case Left(e)       => ZIO.fail(RepositoryError(e))
      case Right(itemId) => ZIO.succeed(itemId)
    }
  }

  override def delete(id: ItemId): IO[RepositoryError, Int] = {

    val deleteQuery = quote {
      itemsSchema.filter(item => item.id == lift(id)).delete
    }

    run(deleteQuery)
      .map(_.toInt)
      .refineOrDie { case e: Exception => RepositoryError(e) }
  }

  override def getAll: IO[RepositoryError, List[Item]] =
    run(itemsSchema).refineOrDie { case e: Exception => RepositoryError(e) }

  override def getById(id: ItemId): IO[RepositoryError, Option[Item]] = {

    val getByIdQuery = quote {
      itemsSchema.filter(item => item.id == lift(id))
    }

    run(getByIdQuery)
      .map(_.headOption)
      .refineOrDie { case e: Exception => RepositoryError(e) }

  }

  override def getByIds(ids: Set[ItemId]): IO[RepositoryError, List[Item]] = {

    val getByIdsQuery = quote { ids: Query[ItemId] =>
      itemsSchema.filter(item => ids.contains(item.id))
    }

    run(getByIdsQuery(liftQuery(ids))).refineOrDie { case e: Exception => RepositoryError(e) }
  }

  override def update(id: ItemId, data: ItemData): IO[RepositoryError, Option[Unit]] = {

    val updateQuery = quote {
      itemsSchema
        .filter(item => item.id == lift(id))
        .updateValue(lift(Item.withData(id, data)))
    }

    run(updateQuery)
      .map(n => if (n > 0) Some(()) else None)
      .refineOrDie { case e: Exception => RepositoryError(e) }
  }

}

object QuillItemRepository {

  val live: RLayer[Quill.Postgres[Literal], QuillItemRepository] = ZLayer {
    for {
      quill <- ZIO.service[Quill.Postgres[Literal]]
    } yield new QuillItemRepository(quill)
  }
}
