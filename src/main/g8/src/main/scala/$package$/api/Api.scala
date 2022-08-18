package $package$.api

import akka.event.Logging._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.interop._
import akka.http.scaladsl.model.StatusCodes.NoContent
import zio._
import zio.logging._
import $package$.api.healthcheck.HealthCheckService
import $package$.application.ApplicationService
import $package$.domain._

trait Api {
  def routes: Route
}

object Api {
  val live: ZLayer[Has[HttpServer.Config]
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
                      .as(EmptyResponse())
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
                        .as(EmptyResponse())
                    )
                  }
                } ~
                put {
                  entity(Directives.as[UpdateItemRequest]) { req =>
                    complete(
                      ApplicationService
                        .updateItem(ItemId(itemId), req.name, req.price)
                        .provide(env)
                        .as(EmptyResponse())
                    )
                  }
                }
            }
          }
        }
    }
  )

  // accessors
  val routes: URIO[Has[Api], Route] = ZIO.access[Has[Api]](a => Route.seal(a.get.routes))
}
