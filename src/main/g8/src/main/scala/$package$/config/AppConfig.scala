package $package$.config

import akka.http.interop.HttpServer

import zio.config
import zio.config.magnolia

final case class AppConfig(api: HttpServer.Config)

object AppConfig {
  val descriptor: config.ConfigDescriptor[AppConfig] = magnolia.descriptor[AppConfig]
}
