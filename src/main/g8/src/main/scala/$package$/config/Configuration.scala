package $package$.config

import com.typesafe.config.ConfigFactory
import zio._
import zio.config._
import zio.config.ConfigDescriptor._
import zio.config.typesafe.TypesafeConfigSource

object Configuration {

  final case class ApiConfig(host: String, port: Int)

  object ApiConfig {

    private val serverConfigDescription =
      nested("api") {
        string("host").default("0.0.0.0") <*>
        int("port").default(8090)
      }.to[ApiConfig]

    val layer = ZLayer(
      read(
        serverConfigDescription.from(
          TypesafeConfigSource.fromTypesafeConfig(
            ZIO.attempt(ConfigFactory.defaultApplication())
          )
        )
      )
    )
  }
}
