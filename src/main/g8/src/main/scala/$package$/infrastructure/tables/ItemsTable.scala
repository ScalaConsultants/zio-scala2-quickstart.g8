package $package$.infrastructure.tables

import $package$.domain._
import $package$.infrastructure.EntityIdMappers
import $package$.infrastructure.Profile

trait ItemsTable extends EntityIdMappers {
  self: Profile =>
  import profile.api._

  class Items(tag: Tag) extends Table[Item](tag, "ITEMS") {
    def id    = column[ItemId]("ID", O.PrimaryKey, O.AutoInc)
    def name  = column[String]("NAME")
    def price = column[BigDecimal]("PRICE")
    def *     = (id, name, price).<>((Item.apply _).tupled, Item.unapply)
  }

  val table = TableQuery[self.Items]
}
