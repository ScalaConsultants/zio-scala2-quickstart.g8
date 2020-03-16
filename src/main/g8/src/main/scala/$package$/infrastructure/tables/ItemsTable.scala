package $package$.infrastructure.tables

import $package$.domain._
import $package$.infrastructure.EntityIdMappers._
import slick.jdbc.H2Profile.api._

object ItemsTable {

  case class LiftedItem(id: Rep[Option[ItemId]], name: Rep[String], price: Rep[BigDecimal])

  implicit object ItemShape extends CaseClassShape(LiftedItem.tupled, Item.tupled)

  class Items(tag: Tag) extends Table[Item](tag, "ITEMS") {
    def id    = column[ItemId]("ID", O.PrimaryKey, O.AutoInc)
    def name  = column[String]("NAME")
    def price = column[BigDecimal]("PRICE")
    def *     = LiftedItem(id.?, name, price)
  }

  val table = TableQuery[ItemsTable.Items]
}
