package $package$.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes, Uri }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directives, Route }
import $package$.application.ApplicationService
import $package$.domain._
import $package$.interop.akka._
import spray.json.{ deserializationError, DefaultJsonProtocol, JsNumber, JsObject, JsValue, JsonFormat, RootJsonFormat }
import zio.{ Has, ZLayer }
import zio.Runtime
import zio.internal.Platform
import zio.config.Config
import zio.config.magnolia.ConfigDescriptorProvider.description

object Api {

  final case class ApiConfig(host: String, port: Int)
  val configDescr = description[ApiConfig]

  trait Service {
    def routes: Route
  }

  final case class CreateItemRequest(name: String, price: BigDecimal)
  final case class UpdateItemRequest(name: String, price: BigDecimal)
  final case class PartialUpdateItemRequest(name: Option[String], price: Option[BigDecimal])

  trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit object ItemIdFormat extends JsonFormat[ItemId] {
      def write(m: ItemId): JsValue = JsNumber(m.value)
      def read(json: JsValue): ItemId = json match {
        case JsNumber(n) => ItemId(n.longValue)
        case _           => deserializationError("Number expected")
      }
    }
    implicit val itemFormat: RootJsonFormat[Item]                           = jsonFormat3(Item)
    implicit val createItemRequestFormat: RootJsonFormat[CreateItemRequest] = jsonFormat2(CreateItemRequest)
    implicit val updateItemRequestFormat: RootJsonFormat[UpdateItemRequest] = jsonFormat2(UpdateItemRequest)
    implicit val partialUpdateItemRequestFormat: RootJsonFormat[PartialUpdateItemRequest] = jsonFormat2(
      PartialUpdateItemRequest
    )
  }

  val live: ZLayer[Config[ApiConfig] with ItemRepository, Nothing, Api] = ZLayer.fromFunction(env =>
    new Service with JsonSupport with ZioSupport {

      def routes: Route = itemRoute

      implicit val domainErrorMapper = new ErrorMapper[DomainError] {
        def toHttpResponse(e: DomainError): HttpResponse = e match {
          case RepositoryError(cause) => HttpResponse(StatusCodes.InternalServerError)
          case ValidationError(msg)   => HttpResponse(StatusCodes.BadRequest)
        }
      }

      val itemRoute: Route =
        pathPrefix("items") {
          pathEnd {
            get {
              complete(ApplicationService.getItems.provide(env))
            } ~
            post {
              extractScheme { scheme =>
                extractHost { host =>
                  entity(Directives.as[CreateItemRequest]) { req =>
                    ApplicationService
                      .addItem(req.name, req.price)
                      .provide(env)
                      .map { id =>
                        respondWithHeader(
                          Location(
                            Uri(scheme = scheme)
                              .withAuthority(host, env.get.config.port)
                              .withPath(Uri.Path(s"/items/\${id.value}"))
                          )
                        ) {
                          complete {
                            HttpResponse(StatusCodes.Created)
                          }
                        }
                      }
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
  )

}
