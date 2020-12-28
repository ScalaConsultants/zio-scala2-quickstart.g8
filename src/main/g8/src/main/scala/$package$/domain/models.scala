package $package$.domain

final case class ItemId(value: Long) extends AnyVal

final case class ItemData(name: String, price: BigDecimal)

final case class DbStatus(status: Boolean)

final case class Item(id: ItemId, name: String, price: BigDecimal) {
  def data: ItemData =
    ItemData(name, price)
}
object ItemId {
  implicit val decoder: JsonDecoder[ItemId] = DeriveJsonDecoder.gen[ItemId]
  implicit val encoder: JsonEncoder[ItemId] = DeriveJsonEncoder.gen[ItemId]
}

object ItemData {
  implicit val decoder: JsonDecoder[ItemData] = DeriveJsonDecoder.gen[ItemData]
  implicit val encoder: JsonEncoder[ItemData] = DeriveJsonEncoder.gen[ItemData]
}

object DbStatus {
  implicit val decoder: JsonDecoder[DbStatus] = DeriveJsonDecoder.gen[DbStatus]
}

object Item {
  implicit val decoder: JsonDecoder[Item] = DeriveJsonDecoder.gen[Item]
  implicit val encoder: JsonEncoder[Item] = DeriveJsonEncoder.gen[Item]

  def withData(id: ItemId, data: ItemData): Item = Item(
    id,
    data.name,
    data.price
  )
}
