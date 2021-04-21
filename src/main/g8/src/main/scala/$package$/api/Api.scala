package $package$.api

import akka.event.Logging._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.interop._
import akka.http.scaladsl.model.StatusCodes.NoContent
import play.api.libs.json.JsObject
import zio._
import zio.logging._
import $package$.api.healthcheck.HealthCheckService
import $package$.application.ApplicationService
import $package$.domain._
$if(add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
import zio.interop.reactivestreams._
import akka.stream.scaladsl.Source
$endif$
$if(add_server_sent_events_endpoint.truthy)$
import akka.http.scaladsl.model.sse.ServerSentEvent
import scala.concurrent.duration._
$endif$
$if(add_websocket_endpoint.truthy)$
import akka.stream.scaladsl.{ Flow, Sink }
import akka.actor.ActorSystem
import akka.http.javadsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import scala.util.{ Try, Success, Failure }
$endif$

trait Api {
  def routes: Route
}

object Api {

  val live: ZLayer[Has[HttpServer.Config]$if(add_websocket_endpoint.truthy)$ with Has[ActorSystem]$endif$
    with Has[ApplicationService] with Logging with Has[HealthCheckService], Nothing, Has[Api]] = ZLayer.fromFunction(env =>
    new Api with JsonSupport with ZIOSupport {

      def routes: Route = itemRoute

      implicit val domainErrorResponse: ErrorResponse[DomainError] = {
        case RepositoryError(_) => HttpResponse(StatusCodes.InternalServerError)
        case ValidationError(_) => HttpResponse(StatusCodes.BadRequest)
      }

      val itemRoute: Route =
        path("healthcheck") {
          get {
            complete(HealthCheckService.healthCheck.provide(env))
          } ~ head(complete(NoContent))
        } ~ pathPrefix("items") {
          logRequestResult(("items", InfoLevel)) {
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

            logRequestResult(("sse/items", InfoLevel)) {
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
          } $endif$ $if(add_websocket_endpoint.truthy)$ ~
          pathPrefix("ws" / "items") {
            logRequestResult(("ws/items", InfoLevel)) {
              val greeterWebSocketService =
                Flow[Message].flatMapConcat {
                  case tm: TextMessage if tm.getStrictText == "deleted" =>
                    Source.futureSource(
                      unsafeRunToFuture(
                        ApplicationService.deletedEvents.toPublisher
                          .map(p =>
                            Source
                              .fromPublisher(p)
                              .map(itemId => TextMessage(s"deleted: \${itemId.value}"))
                          )
                          .provide(env)
                      )
                    )
                  case tm: TextMessage =>
                    Try(tm.getStrictText.toLong) match {
                      case Success(value) =>
                        Source.futureSource(
                          unsafeRunToFuture(
                            ApplicationService
                              .getItem(ItemId(value))
                              .bimap(
                                _.asThrowable,
                                o => Source(o.toList.map(i => TextMessage(i.toString)))
                              )
                              .provide(env)
                          )
                        )
                      case Failure(_) => Source.empty
                    }
                  case bm: BinaryMessage =>
                    bm.getStreamedData.runWith(Sink.ignore, env.get[ActorSystem])
                    Source.empty
                }

              handleWebSocketMessages(greeterWebSocketService)
            }
          }
       $endif$
    }
  )

  // accessors
  val routes: URIO[Has[Api], Route] = ZIO.access[Has[Api]](a => Route.seal(a.get.routes))
}
