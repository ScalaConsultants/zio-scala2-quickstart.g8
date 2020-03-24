package $package$.api

import $package$.domain.{ Item, ItemId }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{ Format, Json }
import play.api.libs.functional.syntax._

final case class CreateItemRequest(name: String, price: BigDecimal)
final case class UpdateItemRequest(name: String, price: BigDecimal)
final case class PartialUpdateItemRequest(name: Option[String], price: Option[BigDecimal])

trait JsonSupport extends PlayJsonSupport {
  implicit val itemIdFormat                   = Json.valueFormat[ItemId]
  implicit val itemFormat                     = Json.format[Item]
  implicit val createItemRequestFormat        = Json.format[CreateItemRequest]
  implicit val updateItemRequestFormat        = Json.format[UpdateItemRequest]
  implicit val partialUpdateItemRequestFormat = Json.format[PartialUpdateItemRequest]
}

object JsonSupport extends JsonSupport
