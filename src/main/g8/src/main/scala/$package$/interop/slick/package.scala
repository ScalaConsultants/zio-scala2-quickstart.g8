package $package$.interop

import zio.Has

package object slick {
  type DatabaseProvider = Has[DatabaseProvider.Service]
}
