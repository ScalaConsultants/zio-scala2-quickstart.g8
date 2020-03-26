package $package$.infrastructure.tables

import $package$.domain._
import $package$.infrastructure.EntityIdMappers._
import slick.jdbc.H2Profile.api._

object ItemsTable {

  class Items(tag: Tag) extends Table[Item](tag, "ITEMS") {
    def id    = column[ItemId]("ID", O.PrimaryKey, O.AutoInc)
    def name  = column[String]("NAME")
    def price = column[BigDecimal]("PRICE")
    def *     = (id, name, price) <> ((Item.apply _).tupled, Item.unapply _)
  }

  val table = TableQuery[ItemsTable.Items]
}
