package $package$.infrastructure

import zio._

package object flyway {
  type FlywayProvider = Has[FlywayProvider.Service]
}
