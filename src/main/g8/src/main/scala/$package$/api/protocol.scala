package $package$.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import $package$.domain.{ Item, ItemId }
import spray.json.{ deserializationError, DefaultJsonProtocol, JsNumber, JsValue, JsonFormat, RootJsonFormat }

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
  implicit val itemFormat: RootJsonFormat[Item]                           = jsonFormat3(Item.apply)
  implicit val createItemRequestFormat: RootJsonFormat[CreateItemRequest] = jsonFormat2(CreateItemRequest)
  implicit val updateItemRequestFormat: RootJsonFormat[UpdateItemRequest] = jsonFormat2(UpdateItemRequest)
  implicit val partialUpdateItemRequestFormat: RootJsonFormat[PartialUpdateItemRequest] = jsonFormat2(
    PartialUpdateItemRequest
  )
}

object JsonSupport extends JsonSupport
