package com.example.domain

final case class ItemId(value: Long) extends AnyVal

final case class ItemData(name: String, price: BigDecimal)

final case class Item(id: ItemId, name: String, price: BigDecimal) {
  def data: ItemData =
    ItemData(name, price)
}

object Item {
  def withData(id: ItemId, data: ItemData): Item = Item(
    id,
    data.name,
    data.price
  )
}
