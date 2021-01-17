package $package$.infrastructure

import $package$.ITSpec.ITSpec
import $package$.domain.{ ItemRepository, _ }
import $package$.infrastructure.flyway.FlywayProvider
import zio.ZIO
import zio.test.Assertion._
import zio.test.{ suite, testM, _ }
import zio.test.TestAspect.before
object ItItemRepositorySpec extends ITSpec(Some("items")) {

  val migrateDbSchema =
    FlywayProvider.flyway
      .flatMap(_.migrate)
      .toManaged_

  def flippingFailure(value: Any): Exception =
    new Exception(
      "Flipping failed! The referred effect was successful with `" + value + "` result of `" + value.getClass + "` type!"
    )

  private def allItems: ZIO[ItemRepository, Throwable, List[Item]] = ItemRepository.getAll.mapError(_.asThrowable)

  migrateDbSchema.useNow
  val spec: ITSpec =
    suite("Item Repository")(
      //  def addItem
      testM("Add correct item ") {
        val name: String = "name"
        val price: BigDecimal = 100.0
        for {
          _: ItemId     <- ItemRepository.add(ItemData(name, price))
          contentsCheck <- assertM(allItems)(equalTo(List(Item(ItemId(1), "name", 100.0))))
        } yield contentsCheck
      },
      testM("Should add different item and fail in assertion ") {
        val name: String = "name1"
        val price: BigDecimal = 100.0
        for {
          _: ItemId     <- ItemRepository.add(ItemData(name, price))
          contentsCheck <- assertM(allItems)(equalTo(List(Item(ItemId(1), "name", 100.0))))
        } yield !contentsCheck
      },
      $if(slick.truthy)$
      testM("Should not allow to add wrong data to db") {
        val name: String = ""
        val price: BigDecimal = null
        migrateDbSchema.useNow
        for {
          error <- ItemRepository.add(ItemData(name, price)).flip.orDieWith(flippingFailure)
        } yield assert(error.toString)(equalTo("RepositoryError(java.lang.NullPointerException)"))
      },
      $endif$
        $if(doobie.truthy)$
        testM("Should not allow to add wrong data to db") {
          val name: String      = ""
          val price: BigDecimal = null
          migrateDbSchema.useNow
          for {
            error <- ItemRepository.add(ItemData(name, price)).flip.orDieWith(flippingFailure)
          } yield assert(error.toString)(equalTo("RepositoryError(java.lang.RuntimeException: oops, null)"))
        },
      $endif$
      $if(add_caliban_endpoint.truthy) $
      // def getItemByName
      testM("Get correct item by name ") {
        val name: String      = "name"
        val price: BigDecimal = 100.0
        for {
          _: ItemId <- ItemRepository.add(ItemData(name, price))
          _: ItemId <- ItemRepository.add(ItemData(name, price + 5))
          item      <- ItemRepository.getByName(name)
        } yield assert(item)(equalTo(List(Item(ItemId(1), name, 100.00), Item(ItemId(2), name, 105.00))))
      },
      testM("Return nothing if there is no item with the same name") {
        for {
          item <- ItemRepository.getByName("name")
        } yield assert(item)(equalTo(List()))
      },
      // def getItemsCheaperThan
      testM("Get cheaper items") {
        val name: String = "name"
        val price: BigDecimal = 100.0
        for {
          _: ItemId <- ItemRepository.add(ItemData(name, price))
          _: ItemId <- ItemRepository.add(ItemData(name, price + 5))
          _: ItemId <- ItemRepository.add(ItemData(name, price + 15))
          _: ItemId <- ItemRepository.add(ItemData(name, price + 45))
          _: ItemId <- ItemRepository.add(ItemData(name, price + 75))
          item      <- ItemRepository.getCheaperThan(120.0)
        } yield assert(item)(
          equalTo(List(Item(ItemId(1), name, 100.00), Item(ItemId(2), name, 105.00), Item(ItemId(3), name, 115.00)))
        )
      },
      $endif$
      // def getItem
      testM ("Get correct item ") {
        val name: String      = "name"
        val price: BigDecimal = 100.0
        for {
          _: ItemId <- ItemRepository.add(ItemData(name, price))
          item      <- ItemRepository.getById(ItemId(1))
        } yield assert(item)(equalTo(Some(Item(ItemId(1), name, 100.00))))
      },
      testM("Get error if  item not exist ") {
        for {
          error <- ItemRepository.getById(ItemId(1))
        } yield assert(error)(equalTo(None))
      },
      //  def getItems
      testM("Get all items ") {
        val name: String = "name"
        val price: BigDecimal = 100.0
        for {
          _: ItemId     <- ItemRepository.add(ItemData(name, price))
          _: ItemId     <- ItemRepository.add(ItemData(name, price + 5))
          _             <- ItemRepository.getAll
          contentsCheck <- assertM(allItems)(
                             equalTo(List(Item(ItemId(1), name, 100.00), Item(ItemId(2), name, 105.00)))
                           )
        } yield contentsCheck
      },
      //  def getItems
      testM("Get empty item list ") {
        val name: String = "name"
        val price: BigDecimal = 100.0
        for {
          _: ItemId     <- ItemRepository.add(ItemData(name, price))
          _: ItemId     <- ItemRepository.add(ItemData(name, price + 5))
          _             <- ItemRepository.getAll
          contentsCheck <- assertM(allItems)(
                             equalTo(List(Item(ItemId(1), name, 100.00), Item(ItemId(2), name, 105.00)))
                           )
        } yield contentsCheck
      },
      //  def deleteItem
      testM("Delete item ") {
        val name: String = "name"
        val price: BigDecimal = 100.0
        for {
          _: ItemId     <- ItemRepository.add(ItemData(name, price))
          _: ItemId     <- ItemRepository.add(ItemData(name, price))
          _             <- ItemRepository.delete(ItemId(1))
          contentsCheck <- assertM(allItems)(equalTo(List(Item(ItemId(2), "name", 100.0))))
        } yield contentsCheck
      },
      testM("Get Unit if tried to delete not existing item ") {
        for {
          delete <- ItemRepository.delete(ItemId(1))
        } yield assert(delete)(equalTo((0)))
      },
      //  def  updateItem
      testM("Update item ") {
        val name: String = "name"
        val price: BigDecimal = 100.0
        for {
          _: ItemId     <- ItemRepository.add(ItemData(name, price))
          _             <- ItemRepository.update(ItemId(1), ItemData("dummy", 123.2))
          contentsCheck <- assertM(allItems)(equalTo(List(Item(ItemId(1), "dummy", 123.2))))
        } yield contentsCheck
      },
      testM("Get None if tried to update not existing item ") {
        for {
          update <- ItemRepository.update(ItemId(1), ItemData("dummy", 123.2))
        } yield assert(update)(equalTo(None))
      }
    ) @@ before(FlywayProvider.flyway.flatMap(_.migrate).orDie)
}