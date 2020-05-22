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
        }
    }
  )

  // accessors
  val routes: URIO[Api, Route] = ZIO.access[Api](a => Route.seal(a.get.routes))
}
