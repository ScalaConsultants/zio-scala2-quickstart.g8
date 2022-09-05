package $package$.api

import akka.http.interop.HttpServer
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{ Framing, Sink, Source }
import akka.util.ByteString

import zio._
import zio.test._
import zio.test.Assertion._
import zio.logging.backend.SLF4J

import $package$.domain._
import $package$.api.JsonSupport._
import $package$.api.healthcheck._
import $package$.application.ApplicationService
import $package$.interop.akka.ZioRouteTest

object ApiSpec extends ZioRouteTest {

  private implicit val rt: Runtime[Any] = Runtime.default

  val testLayers
    : ULayer[HttpServer.Config with ApplicationService with HealthCheckService with Api with Annotations] = {

    val logging: ULayer[Unit] = ZLayer.make[Unit](
      Runtime.removeDefaultLoggers,
      SLF4J.slf4j
    )

    val config: ULayer[HttpServer.Config] = ZLayer.succeed(HttpServer.Config("localhost", 8080))

    ZLayer.make[HttpServer.Config with ApplicationService with HealthCheckService with Api with Annotations](
      logging,
      config,
      InMemoryItemRepository.test,
      ApplicationService.live,
      InMemoryHealthCheckService.test,
      Api.live,
      Annotations.live
    )
  }

  private def allItems: ZIO[ApplicationService, Throwable, List[Item]] =
    ApplicationService.getItems.mapError(_.asThrowable)

  private val specs =
    suite("Api")(
      test("Health check on Get to '/healthcheck'") {
        for {
          routes <- Api.routes

          request      = Get("/healthcheck")
          resultCheck <- ZIO.attemptBlocking(request ~> routes ~> check {
                           // Here and in other tests we have to evaluate response on the spot before passing anything to `assert`.
                           // This is due to really tricky nature of how `check` works with the result (no simple workaround found so far)
                           val theStatus = status
                           val theCT     = contentType
                           val theBody   = entityAs[DbStatus]
                           assert(theStatus)(equalTo(StatusCodes.OK)) &&
                           assert(theCT)(equalTo(ContentTypes.`application/json`)) &&
                           assert(theBody)(equalTo(DbStatus(true)))
                         })
        } yield resultCheck
      },
      test("Health check on Head to '/healthcheck'") {
        for {
          routes <- Api.routes

          request      = Head("/healthcheck")
          resultCheck <- ZIO.attemptBlocking(request ~> routes ~> check {
                           // Here and in other tests we have to evaluate response on the spot before passing anything to `assert`.
                           // This is due to really tricky nature of how `check` works with the result (no simple workaround found so far)
                           val theStatus = status
                           val theCT     = contentType
                           assert(theStatus)(equalTo(StatusCodes.NoContent)) &&
                           assert(theCT)(equalTo(ContentTypes.NoContentType))
                         })
        } yield resultCheck
      },
      test("Add item on POST to '/items'") {
        val item = CreateItemRequest("name", 100.0)

        for {
          routes        <- Api.routes
          entity        <- ZIO.fromFuture(_ => Marshal(item).to[MessageEntity])
          request        = Post("/items").withEntity(entity)
          resultCheck   <- ZIO.attemptBlocking(request ~> routes ~> check {
                             // Here and in other tests we have to evaluate response on the spot before passing anything to `assert`.
                             // This is due to really tricky nature of how `check` works with the result (no simple workaround found so far)
                             val theStatus = status
                             val theCT     = contentType
                             val theBody   = entityAs[Item]
                             assert(theStatus)(equalTo(StatusCodes.OK)) &&
                             assert(theCT)(equalTo(ContentTypes.`application/json`)) &&
                             assert(theBody)(equalTo(Item(ItemId(0), "name", 100.0)))
                           })
          contentsCheck <- assertZIO(allItems)(equalTo(List(Item(ItemId(0), "name", 100.0))))
        } yield resultCheck && contentsCheck
      },
      test("Not allow malformed json on POST to '/items'") {
        val item = EmptyResponse()
        for {
          routes        <- Api.routes
          entity        <- ZIO.fromFuture(_ => Marshal(item).to[MessageEntity])
          request        = Post("/items").withEntity(entity)
          resultCheck   <- ZIO.attemptBlocking(request ~> routes ~> check {
                             val r = response
                             assert(r.status)(equalTo(StatusCodes.BadRequest))
                           })
          contentsCheck <- assertZIO(allItems)(isEmpty)
        } yield resultCheck && contentsCheck
      },
      test("Return all items on GET to '/items'") {
        val items = List(Item(ItemId(0), "name", 100.0), Item(ItemId(1), "name2", 200.0))

        for {
          _             <- ZIO.foreach(items)(i => ApplicationService.addItem(i.name, i.price)).mapError(_.asThrowable)
          routes        <- Api.routes
          resultCheck   <- ZIO.attemptBlocking(Get("/items") ~> routes ~> check {
                             val theStatus = status
                             val theCT     = contentType
                             val theBody   = entityAs[List[Item]]
                             assert(theStatus)(equalTo(StatusCodes.OK)) &&
                             assert(theCT)(equalTo(ContentTypes.`application/json`)) &&
                             assert(theBody)(hasSameElements(items))
                           })
          contentsCheck <- assertZIO(allItems)(hasSameElements(items))
        } yield resultCheck && contentsCheck
      },
      test("Delete item on DELETE to '/items/:id'") {
        val items = List(Item(ItemId(0), "name", 100.0), Item(ItemId(1), "name2", 200.0))

        for {
          _             <- ZIO.foreach(items)(i => ApplicationService.addItem(i.name, i.price)).mapError(_.asThrowable)
          routes        <- Api.routes
          resultCheck   <- ZIO.attemptBlocking(Delete("/items/1") ~> routes ~> check {
                             val s = status
                             assert(s)(equalTo(StatusCodes.OK))
                           })
          contentsCheck <- assertZIO(allItems)(hasSameElements(items.take(1)))
        } yield resultCheck && contentsCheck
      }
    ) @@ TestAspect.sequential

  def firstNElements(request: HttpRequest, route: Route)(n: Long): Task[Seq[String]] =
    ZIO.fromFuture(_ =>
      Source
        .single(request)
        .via(Route.toFlow(route))
        .flatMapConcat(
          _.entity.dataBytes
            .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 100, allowTruncation = true))
            .map(_.utf8String)
            .filter(_.nonEmpty)
        )
        .take(n)
        .runWith(Sink.seq)
    )

  override def spec = specs.provideLayer(testLayers)
}
