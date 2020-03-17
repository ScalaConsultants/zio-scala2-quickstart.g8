package $package$.domain

case class ItemId(value: Long) extends AnyVal

case class Item(id: ItemId, name: String, price: BigDecimal)
