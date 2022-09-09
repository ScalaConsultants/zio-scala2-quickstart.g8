package $package$.config

import com.typesafe.config.{ Config, ConfigFactory }

import zio._
import zio.config.magnolia
import zio.config.typesafe.TypesafeConfig

object AppConfig {

  final case class RootConfig(api: ApiConfig)
  final case class ApiConfig(host: String, port: Int)

  val descriptor: config.ConfigDescriptor[RootConfig] = magnolia.descriptor[RootConfig]

  val effect: UIO[Config] = ZIO.attempt(ConfigFactory.load.resolve).orDie

  val root: ULayer[RootConfig] =
    TypesafeConfig
      .fromTypesafeConfig[RootConfig](effect, descriptor)
      .orDie

  object Api {
    val live: ULayer[ApiConfig] = AppConfig.root.flatMap(cfg => ZLayer.succeed(cfg.get.api))
  }

  object Database {
    // using raw config since it's recommended and the simplest to work with slick
    val live: ULayer[Config] = ZLayer(effect.map(_.getConfig("db")))
  }

}
