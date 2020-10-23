package $package$.domain

import zio.ZLayer
import slick.interop.zio.DatabaseProvider
import zio.blocking.Blocking
import zio.logging.Logging
import $package$.infrastructure.SlickItemRepository

trait PosgresContainer {
  import Postgres._
  val imageName       = "postgres:latest"
  val testCredentials = TestCredentials("posgres", "12345")

  val containerDatabaseProvider: ZLayer[Blocking, Throwable, DatabaseProvider] =
    (ZLayer.identity[Blocking] ++ ZLayer.succeed(testCredentials) >+> postgres(Some(imageName))) >>> postgresDbProviderLayer

  val containerRepository: ZLayer[Blocking, Throwable, ItemRepository] = (Logging.ignore ++ containerDatabaseProvider) >>> SlickItemRepository.live
}