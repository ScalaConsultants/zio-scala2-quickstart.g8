package $package$.api

import akka.event.Logging._
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directives, Route }
import $package$.application.ApplicationService
import $package$.config.ApiConfig
import $package$.domain._
import $package$.interop.akka._
import play.api.libs.json.JsObject
import zio.ZLayer
import zio.config.Config

object Api {

  trait Service {
    def routes: Route
  }

  val live: ZLayer[Config[ApiConfig] with ItemRepository, Nothing, Api] = ZLayer.fromFunction(env =>
    new Service with JsonSupport with ZioSupport {

      def routes: Route = itemRoute

      implicit val domainErrorMapper = new ErrorMapper[DomainError] {
        def toHttpResponse(e: DomainError): HttpResponse = e match {
          case RepositoryError(_) => HttpResponse(StatusCodes.InternalServerError)
          case ValidationError(_) => HttpResponse(StatusCodes.BadRequest)
        }
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

}
