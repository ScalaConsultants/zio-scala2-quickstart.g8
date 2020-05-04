package $package$.api

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import $package$.api.JsonSupport._
import $package$.application.ApplicationService
import $package$.config.ApiConfig
import $package$.domain._
import $package$.interop.akka.ZioRouteTest
import play.api.libs.json.JsObject
import zio._
import zio.blocking._
import zio.test.Assertion._
import zio.test._

object ApiSpec extends ZioRouteTest {

  private val env =
    (ZLayer.succeed(ApiConfig("localhost", 8080)) ++
      InMemoryItemRepository.test) >>>
      Api.live.passthrough ++ Blocking.live

  private def allItems: ZIO[ItemRepository, Throwable, List[Item]] = ApplicationService.getItems.mapError(_.asThrowable)

  private val specs: Spec[ItemRepository with Blocking with Api, TestFailure[Throwable], TestSuccess] =
    suite("Api")(
      testM("Add item on POST to '/items'") {
        val item = CreateItemRequest("name", 100.0)

        for {
          routes  <- Api.routes
          entity  <- ZIO.fromFuture(_ => Marshal(item).to[MessageEntity])
          request = Post("/items").withEntity(entity)
          resultCheck <- effectBlocking(request ~> routes ~> check {
                          // Here and in other tests we have to evaluate response on the spot before passing anything to `assert`.
                          // This is due to really tricky nature of how `check` works with the result (no simple workaround found so far)
                          val theStatus = status
                          val theCT     = contentType
                          val theBody   = entityAs[Item]
                          assert(theStatus)(equalTo(StatusCodes.OK)) &&
                          assert(theCT)(equalTo(ContentTypes.`application/json`)) &&
                          assert(theBody)(equalTo(Item(ItemId(0), "name", 100.0)))
                        })
          contentsCheck <- assertM(allItems)(equalTo(List(Item(ItemId(0), "name", 100.0))))
        } yield resultCheck && contentsCheck
      },
      testM("Not allow malformed json on POST to '/items'") {
        val item = JsObject.empty
        for {
          routes  <- Api.routes
          entity  <- ZIO.fromFuture(_ => Marshal(item).to[MessageEntity])
          request = Post("/items").withEntity(entity)
          resultCheck <- effectBlocking(request ~> routes ~> check {
                          val r = response
                          assert(r.status)(equalTo(StatusCodes.BadRequest))
                        })
          contentsCheck <- assertM(allItems)(isEmpty)
        } yield resultCheck && contentsCheck
      },
      testM("Return all items on GET to '/items'") {
        val items = List(Item(ItemId(0), "name", 100.0), Item(ItemId(1), "name2", 200.0))

        for {
          _      <- ZIO.foreach(items)(i => ApplicationService.addItem(i.name, i.price)).mapError(_.asThrowable)
          routes <- Api.routes
          resultCheck <- effectBlocking(Get("/items") ~> routes ~> check {
                          val theStatus = status
                          val theCT     = contentType
                          val theBody   = entityAs[List[Item]]
                          assert(theStatus)(equalTo(StatusCodes.OK)) &&
                          assert(theCT)(equalTo(ContentTypes.`application/json`)) &&
                          assert(theBody)(hasSameElements(items))
                        })
          contentsCheck <- assertM(allItems)(hasSameElements(items))
        } yield resultCheck && contentsCheck
      },
      testM("Delete item on DELETE to '/items/:id'") {
        val items = List(Item(ItemId(0), "name", 100.0), Item(ItemId(1), "name2", 200.0))

        for {
          _      <- ZIO.foreach(items)(i => ApplicationService.addItem(i.name, i.price)).mapError(_.asThrowable)
          routes <- Api.routes
          resultCheck <- effectBlocking(Delete("/items/1") ~> routes ~> check {
                          val s = status
                          assert(s)(equalTo(StatusCodes.OK))
                        })
          contentsCheck <- assertM(allItems)(hasSameElements(items.take(1)))
        } yield resultCheck && contentsCheck
      }
    )

  def spec = specs.provideLayer(env)
}
