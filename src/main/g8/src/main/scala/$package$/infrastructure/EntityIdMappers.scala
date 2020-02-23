package $package$.infrastructure

import $package$.domain.ItemId
import slick.jdbc.H2Profile.api._

object EntityIdMappers {

  implicit def itemIdMapper: BaseColumnType[ItemId] = MappedColumnType.base[ItemId, Long](
    ent => ent.value,
    value => ItemId(value)
  )

}
