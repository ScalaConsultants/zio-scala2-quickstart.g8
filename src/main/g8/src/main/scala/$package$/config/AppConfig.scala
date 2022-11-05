package $package$.config

import com.typesafe.config.{ Config, ConfigFactory }

import zio._
import zio.config.magnolia
import zio.config.typesafe.TypesafeConfig

object AppConfig {

  final case class RootConfig(api: ApiConfig)
  final case class ApiConfig(host: String, port: Int)

  private val effect: UIO[Config] = ZIO.attempt(ConfigFactory.load.resolve).orDie

  object Root {
    val live: ULayer[RootConfig] =
      TypesafeConfig
        .fromTypesafeConfig[RootConfig](effect, magnolia.descriptor[RootConfig])
        .orDie
  }

  object Api {
    val live: ULayer[ApiConfig] = Root.live.flatMap { rootConfig =>
      ZLayer.succeed(rootConfig.get.api)
    }
  }

  object RawConfig {
    val live: ULayer[Config] = ZLayer(effect)
  }

}
