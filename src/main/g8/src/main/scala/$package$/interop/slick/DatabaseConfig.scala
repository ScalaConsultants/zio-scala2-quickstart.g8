package $package$.interop.slick

import com.typesafe.config.Config

/**
 * The [[underlying]] config has to be scoped to the object with db configuration.
 * For example, if you have a following config file:
 *
 * ```
 * app {
 *   db {
 *     # db settings like url, driver, etc.
 *   }
 * }
 * ```
 * then [[underlying]] should be the `app.db` object.
 */
final case class DatabaseConfig(underlying: Config)
