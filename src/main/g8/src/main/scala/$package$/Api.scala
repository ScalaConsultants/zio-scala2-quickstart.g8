package $package$

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import $package$.application.ApplicationService
import $package$.domain.{ Item, ItemId, ItemRepository }
import $package$.infrastructure._
import $package$.interop.ZioSupport
import spray.json._

case class CreateItemRequest(name: String, price: BigDecimal)
case class UpdateItemRequest(name: String, price: BigDecimal)
case class PartialUpdateItemRequest(name: Option[String], price: Option[BigDecimal])

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object ItemIdFormat extends JsonFormat[ItemId] {
    def write(m: ItemId) = JsNumber(m.value)
    def read(json: JsValue) = json match {
      case JsNumber(n) => ItemId(n.longValue)
      case _ => deserializationError("Number expected")
    }
  }
  implicit val itemFormat = jsonFormat3(Item)
  implicit val createItemRequestFormat = jsonFormat2(CreateItemRequest)
  implicit val updateItemRequestFormat = jsonFormat2(UpdateItemRequest)
  implicit val partialUpdateItemRequestFormat = jsonFormat2(PartialUpdateItemRequest)
}

class Api(env: ItemRepository) extends JsonSupport with ZioSupport {

  lazy val route = itemRoute

  val itemRoute =
    pathPrefix("items") {
      pathEnd {
        get {
          complete(ApplicationService.getItems.provide(env))
        } ~
        post {
          extractScheme { scheme =>
            extractHost { host => 
              entity(Directives.as[CreateItemRequest]) { req =>
                ApplicationService.addItem(req.name, req.price).provide(env).map { id =>
                  respondWithHeader(Location(Uri(scheme = scheme).withHost(host).withPath(Uri.Path(s"items/$id")))) {
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
      path(LongNumber) { itemId =>
        delete {
          complete(ApplicationService.deleteItem(ItemId(itemId)).provide(env).map(_ => JsObject.empty))
        } ~
        get {
          complete(ApplicationService.getItem(ItemId(itemId)).provide(env))
        } ~
        patch {
          entity(Directives.as[PartialUpdateItemRequest]) { req =>
            complete(ApplicationService.partialUpdateItem(ItemId(itemId), req.name, req.price).provide(env).map(_ => JsObject.empty))
          }
        } ~
        put {
          entity(Directives.as[UpdateItemRequest]) { req =>
            complete(ApplicationService.updateItem(ItemId(itemId), req.name, req.price).provide(env).map(_ => JsObject.empty))
          }
        }
      }
    }
}
