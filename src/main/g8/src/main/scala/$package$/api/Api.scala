package $package$.api

import akka.event.Logging._
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directives, Route }
import $package$.application.ApplicationService
import $package$.domain._
import akka.http.interop._
import play.api.libs.json.JsObject
import zio._
import zio.config.Config
$if(add_server_sent_events_endpoint.truthy)$
import java.time.LocalTime
import akka.NotUsed
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.Source
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import scala.concurrent.duration._
import zio.interop.reactivestreams._
$endif$

object Api {

  trait Service {
    def routes: Route
  }

  val live: ZLayer[Config[HttpServer.Config] with ItemRepository, Nothing, Api] = ZLayer.fromFunction(env =>
    new Service with JsonSupport with ZIOSupport {

      def routes: Route = itemRoute

      implicit val domainErrorResponse: ErrorResponse[DomainError] = {
        case RepositoryError(_) => HttpResponse(StatusCodes.InternalServerError)
        case ValidationError(_) => HttpResponse(StatusCodes.BadRequest)
      }

      val itemRoute: Route =
        pathPrefix("items") {
          logRequestResult("items", InfoLevel) {
            pathEnd {
              get {
                complete(ApplicationService.getItems.provide(env))
              } ~
              post {
                entity(Directives.as[CreateItemRequest]) { req =>
                  ApplicationService
                    .addItem(req.name, req.price)
                    .provide(env)
                    .map { id =>
                      complete {
                        Item(id, req.name, req.price)
                      }
                    }
                }
              }
            } ~
            path(LongNumber) {
              itemId =>
                delete {
                  complete(
                    ApplicationService
                      .deleteItem(ItemId(itemId))
                      .provide(env)
                      .as(JsObject.empty)
                  )
                } ~
                get {
                  complete(ApplicationService.getItem(ItemId(itemId)).provide(env))
                } ~
                patch {
                  entity(Directives.as[PartialUpdateItemRequest]) { req =>
                    complete(
                      ApplicationService
                        .partialUpdateItem(ItemId(itemId), req.name, req.price)
                        .provide(env)
                        .as(JsObject.empty)
                    )
                  }
                } ~
                put {
                  entity(Directives.as[UpdateItemRequest]) { req =>
                    complete(
                      ApplicationService
                        .updateItem(ItemId(itemId), req.name, req.price)
                        .provide(env)
                        .as(JsObject.empty)
                    )
                  }
                }
            }
          }
        } $if(add_server_sent_events_endpoint.truthy)$ ~
          pathPrefix("sse" / "items") {
            import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._

            logRequestResult("sse/items", InfoLevel) {
              pathPrefix("deleted") {
                get {
                  complete {
                    ApplicationService.deletedEvents.toPublisher
                      .map(p =>
                        Source
                          .fromPublisher(p)
                          .map(itemId => ServerSentEvent(itemId.value.toString))
                          .keepAlive(1.second, () => ServerSentEvent.heartbeat)
                      )
                      .provide(env)
                  }
                }
              }
            }
          } $endif$
    }
  )

  // accessors
  val routes: URIO[Api, Route] = ZIO.access[Api](a => Route.seal(a.get.routes))
}
