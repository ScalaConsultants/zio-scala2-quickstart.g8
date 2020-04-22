package $package$.config

import zio.config.magnolia.DeriveConfigDescriptor

final case class AppConfig(api: ApiConfig)
final case class ApiConfig(host: String, port: Int)

object AppConfig {
  val descriptor = DeriveConfigDescriptor.descriptor[AppConfig]
}
