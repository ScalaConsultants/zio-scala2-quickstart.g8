package $package$.config

import zio.config.magnolia.ConfigDescriptorProvider

final case class AppConfig(api: ApiConfig)
final case class ApiConfig(host: String, port: Int)

object AppConfig {
  val description = ConfigDescriptorProvider.description[AppConfig]
}
