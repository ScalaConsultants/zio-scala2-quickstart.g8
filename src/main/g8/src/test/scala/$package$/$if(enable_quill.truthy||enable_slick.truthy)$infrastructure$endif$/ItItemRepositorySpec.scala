package $package$.infrastructure

import org.flywaydb.core.api.FlywayException

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.before

import $package$.ITSpec
import $package$.domain.{ ItemRepository, _ }
import $package$.infrastructure.flyway.{ FlywayProvider, MigrateResult }

object ItItemRepositorySpec extends ITSpec(Some("items")) {

  val migrateDbSchema: ZIO[FlywayProvider, FlywayException, MigrateResult] =
    FlywayProvider.flyway.flatMap(_.migrate)

  def flippingFailure(value: Any): Exception = new Exception(
    "Flipping failed! The referred effect was successful with `" + value + "` result of `" + value.getClass + "` type!"
  )

  private def allItems: ZIO[ItemRepository, Throwable, List[Item]] = ItemRepository.getAll.mapError(_.asThrowable)

  ZIO.scoped(migrateDbSchema)

  private val suites = suite("Item Repository")(
    //  def addItem
    test("Add correct item") {
      val name: String      = "name"
      val price: BigDecimal = 100.0
      for {
        _: ItemId     <- ItemRepository.add(ItemData(name, price))
        contentsCheck <- assertZIO(allItems)(equalTo(List(Item(ItemId(1), "name", 100.0))))
      } yield contentsCheck
    },
    test("Should add different item and fail in assertion") {
      val name: String      = "name1"
      val price: BigDecimal = 100.0
      for {
        _: ItemId     <- ItemRepository.add(ItemData(name, price))
        contentsCheck <- assertZIO(allItems)(equalTo(List(Item(ItemId(1), "name", 100.0))))
      } yield !contentsCheck
    },
    test("Should not allow to add wrong data to db") {
      val name: String      = ""
      val price: BigDecimal = null
      ZIO.scoped(migrateDbSchema)
      for {
        error <- ItemRepository.add(ItemData(name, price)).flip.orDieWith(flippingFailure)
      } yield assert(error.toString)(startsWithString("RepositoryError(java.lang.NullPointerException"))
    },
    // def getItem
    test("Get correct item") {
      val name: String      = "name"
      val price: BigDecimal = 100.0
      for {
        _: ItemId <- ItemRepository.add(ItemData(name, price))
        item      <- ItemRepository.getById(ItemId(1))
      } yield assert(item)(isSome(equalTo(Item(ItemId(1), name, 100.00))))
    },
    test("Get error if item not exist") {
      for {
        error <- ItemRepository.getById(ItemId(1))
      } yield assert(error)(isNone)
    },
    //  def getItems
    test("Get all items") {
      val name: String      = "name"
      val price: BigDecimal = 100.0
      for {
        _: ItemId     <- ItemRepository.add(ItemData(name, price))
        _: ItemId     <- ItemRepository.add(ItemData(name, price + 5))
        _             <- ItemRepository.getAll
        contentsCheck <- assertZIO(allItems)(
                           equalTo(List(Item(ItemId(1), name, 100.00), Item(ItemId(2), name, 105.00)))
                         )
      } yield contentsCheck
    },
    //  def getItems
    test("Get empty item list") {
      val name: String      = "name"
      val price: BigDecimal = 100.0
      for {
        _: ItemId     <- ItemRepository.add(ItemData(name, price))
        _: ItemId     <- ItemRepository.add(ItemData(name, price + 5))
        _             <- ItemRepository.getAll
        contentsCheck <- assertZIO(allItems)(
                           equalTo(List(Item(ItemId(1), name, 100.00), Item(ItemId(2), name, 105.00)))
                         )
      } yield contentsCheck
    },
    //  def deleteItem
    test("Delete item") {
      val name: String      = "name"
      val price: BigDecimal = 100.0
      for {
        _: ItemId     <- ItemRepository.add(ItemData(name, price))
        _: ItemId     <- ItemRepository.add(ItemData(name, price))
        _             <- ItemRepository.delete(ItemId(1))
        contentsCheck <- assertZIO(allItems)(equalTo(List(Item(ItemId(2), "name", 100.0))))
      } yield contentsCheck
    },
    test("Get Unit if tried to delete not existing item") {
      for {
        delete <- ItemRepository.delete(ItemId(1))
      } yield assert(delete)(equalTo((0)))
    },
    //  def  updateItem
    test("Update item ") {
      val name: String      = "name"
      val price: BigDecimal = 100.0
      for {
        _: ItemId     <- ItemRepository.add(ItemData(name, price))
        _             <- ItemRepository.update(ItemId(1), ItemData("dummy", 123.2))
        contentsCheck <- assertZIO(allItems)(equalTo(List(Item(ItemId(1), "dummy", 123.2))))
      } yield contentsCheck
    },
    test("Get None if tried to update not existing item") {
      for {
        update <- ItemRepository.update(ItemId(1), ItemData("dummy", 123.2))
      } yield assert(update)(isNone)
    }
  ) @@ before(FlywayProvider.flyway.flatMap(_.migrate))

  override val spec: Spec[TestEnvironment with Scope, Object] = suites.provide(itLayers)

}
