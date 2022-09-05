package $package$.api

import akka.event.Logging._
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directives, Route }
import akka.http.interop._
import akka.http.scaladsl.model.StatusCodes.NoContent

import zio._

import $package$.domain._
import $package$.application.ApplicationService
import $package$.api.healthcheck.HealthCheckService

trait Api {
  def routes: Route
}

object Api {

  class Impl(healthCheck: HealthCheckService, application: ApplicationService)
      extends Api
      with JsonSupport
      with ZIOSupport {

    override def routes: Route = itemRoute

    private implicit val domainErrorResponse: ErrorResponse[DomainError] = {
      case RepositoryError(_) => HttpResponse(StatusCodes.InternalServerError)
      case ValidationError(_) => HttpResponse(StatusCodes.BadRequest)
    }

    private implicit val rt: Runtime[Any] = Runtime.default

    private val healthCheckLayer = ZLayer.succeed(healthCheck)
    private val applicationLayer = ZLayer.succeed(application)

    val itemRoute: Route =
      path("healthcheck") {
        get {
          complete(HealthCheckService.healthCheck.provide(healthCheckLayer))
        } ~ head(complete(NoContent))
      } ~ pathPrefix("items") {
        logRequestResult(("items", InfoLevel)) {
          pathEnd {
            get {
              complete(ApplicationService.getItems.provide(applicationLayer))
            } ~
            post {
              entity(Directives.as[CreateItemRequest]) { req =>
                ApplicationService
                  .addItem(req.name, req.price)
                  .provide(applicationLayer)
                  .map { id =>
                    complete {
                      Item(id, req.name, req.price)
                    }
                  }
              }
            }
          } ~
          path(LongNumber) { itemId =>
            delete {
              complete(
                ApplicationService
                  .deleteItem(ItemId(itemId))
                  .provide(applicationLayer)
                  .as(EmptyResponse())
              )
            } ~
            get {
              complete(ApplicationService.getItem(ItemId(itemId)).provide(applicationLayer))
            } ~
            patch {
              entity(Directives.as[PartialUpdateItemRequest]) { req =>
                complete(
                  ApplicationService
                    .partialUpdateItem(ItemId(itemId), req.name, req.price)
                    .provide(applicationLayer)
                    .as(EmptyResponse())
                )
              }
            } ~
            put {
              entity(Directives.as[UpdateItemRequest]) { req =>
                complete(
                  ApplicationService
                    .updateItem(ItemId(itemId), req.name, req.price)
                    .provide(applicationLayer)
                    .as(EmptyResponse())
                )
              }
            }
          }
        }
      }
  }

  // accessors
  val routes: URIO[Api, Route] = ZIO.environmentWithZIO[Api](api => ZIO.succeed(Route.seal(api.get.routes)))

  val live: URLayer[HttpServer.Config with ApplicationService with HealthCheckService, Api] = ZLayer {
    for {
      healthCheck <- ZIO.service[HealthCheckService]
      application <- ZIO.service[ApplicationService]
    } yield new Impl(healthCheck, application)
  }

}
