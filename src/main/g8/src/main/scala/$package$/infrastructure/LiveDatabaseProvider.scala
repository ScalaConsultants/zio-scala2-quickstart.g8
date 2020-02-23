package $package$.infrastructure

import $package$.interop.slick.DatabaseProvider
import zio.ZIO
import slick.jdbc.H2Profile.backend._

trait LiveDatabaseProvider extends DatabaseProvider {
  override val databaseProvider = new DatabaseProvider.Service {
    override val db = ZIO.effectTotal(Database.forURL("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", driver="org.h2.Driver"))
  }
}
