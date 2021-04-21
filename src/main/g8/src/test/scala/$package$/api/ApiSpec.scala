package $package$.api

import akka.http.interop.HttpServer
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Framing, Sink, Source}
import akka.util.ByteString
import play.api.libs.json.JsObject
import zio._
import zio.blocking._
import zio.clock.Clock
import $package$.api.JsonSupport._
import $package$.api.healthcheck._
import $package$.application.ApplicationService
import $package$.domain._
import $package$.interop.akka.ZioRouteTest
import zio.logging._
import zio.logging.slf4j.Slf4jLogger
$if(add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
import zio.test.TestAspect.ignore
import zio.duration.Duration
$endif$
import zio.test.Assertion._
import zio.test._
$if(add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
import scala.concurrent.duration._
$endif$
$if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
import $package$.infrastructure.InMemoryEventSubscriber
$endif$

object ApiSpec extends ZioRouteTest {

  private val loggingLayer: ULayer[Logging] = Slf4jLogger.make { (context, message) =>
      val logFormat = "[correlation-id = %s] %s"
      val correlationId = LogAnnotation.CorrelationId.render(
        context.get(LogAnnotation.CorrelationId)
      )
      logFormat.format(correlationId, message)
    }

  val apiLayer = (
    ((InMemoryItemRepository.test$if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$ ++ InMemoryEventSubscriber.test$endif$) >>> ApplicationService.live) ++ 
      loggingLayer ++ 
      $if(add_websocket_endpoint.truthy)$ZLayer.succeed(system) ++ $endif$
      InMemoryHealthCheckService.test ++ 
      ZLayer.succeed(HttpServer.Config("localhost", 8080))
  ) >>> Api.live.passthrough

  private val env = apiLayer ++ Blocking.live ++ Clock.live ++ Annotations.live

  private def allItems: ZIO[Has[ApplicationService], Throwable, List[Item]] = ApplicationService.getItems.mapError(_.asThrowable)

  private val specs: Spec[Has[ApplicationService] with Blocking with Has[Api] with Clock with Annotations, TestFailure[Throwable], TestSuccess] =
    suite("Api")(
      testM("Health check on Get to '/healthcheck'") {
        for {
          routes <- Api.routes

          request = Get("/healthcheck")
          resultCheck <- effectBlocking(request ~> routes ~> check {
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
      testM("Health check on Head to '/healthcheck'") {
        for {
          routes <- Api.routes

          request = Head("/healthcheck")
          resultCheck <- effectBlocking(request ~> routes ~> check {
            // Here and in other tests we have to evaluate response on the spot before passing anything to `assert`.
            // This is due to really tricky nature of how `check` works with the result (no simple workaround found so far)
            val theStatus = status
            val theCT     = contentType
            assert(theStatus)(equalTo(StatusCodes.NoContent)) &&
              assert(theCT)(equalTo(ContentTypes.NoContentType))
          })
        } yield resultCheck
      },
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
          _ <- ZIO.foreach(items)(i => ApplicationService.addItem(i.name, i.price)).mapError(_.asThrowable)
          routes <- Api.routes
          resultCheck <- effectBlocking(Delete("/items/1") ~> routes ~> check {
            val s = status
            assert(s)(equalTo(StatusCodes.OK))
          })
          contentsCheck <- assertM(allItems)(hasSameElements(items.take(1)))
        } yield resultCheck && contentsCheck


     //TODO: In this moment Delete event is not working at all need to be fixed
      } $if(add_server_sent_events_endpoint.truthy)$ ,
      testM("Notify about deleted items via SSE endpoint") {

        val items = List(Item(ItemId(0), "name", 100.0), Item(ItemId(1), "name2", 200.0))

        for {
          _      <- ZIO.foreach(items)(i => ApplicationService.addItem(i.name, i.price)).mapError(_.asThrowable)
          routes <- Api.routes
          fiber    <- firstNElements(Get("/sse/items/deleted"), routes)(3).fork
          _        <- ZIO.sleep(Duration.fromScala(1.second))
          _        <- ApplicationService.deleteItem(ItemId(1)).mapError(_.asThrowable)
          _        <- ApplicationService.deleteItem(ItemId(2)).mapError(_.asThrowable)
          messages <- fiber.join
        } yield assert(messages.filterNot(_ == "data:"))(hasSameElements(List("data:1", "data:2")))
      }@@ ignore $endif$ $if(add_websocket_endpoint.truthy)$,
      testM("Notify about deleted items via WS endpoint") {
        import akka.http.scaladsl.testkit.WSProbe
        val items = List(Item(ItemId(0), "name", 100.0), Item(ItemId(1), "name2", 200.0))
        val wsClient = WSProbe()
        for {
          _      <- ZIO.foreach(items)(i => ApplicationService.addItem(i.name, i.price)).mapError(_.asThrowable)
          routes <- Api.routes

          resultFiber <- effectBlocking {
                          WS("/ws/items", wsClient.flow) ~> routes ~> check {
                            val isUpgrade = isWebSocketUpgrade

                            wsClient.sendMessage("deleted")
                            wsClient.expectMessage("deleted: 1")
                            wsClient.expectMessage("deleted: 2")
                            assert(isUpgrade)(isTrue)
                          }
                        }.fork
          _      <- ZIO.sleep(Duration.fromScala(1.second))
          _      <- ApplicationService.deleteItem(ItemId(1)).mapError(_.asThrowable)
          _      <- ApplicationService.deleteItem(ItemId(2)).mapError(_.asThrowable)
          result <- resultFiber.join
        } yield result
      } @@ ignore $endif$
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

  def spec = specs.provideLayer(env)
}
