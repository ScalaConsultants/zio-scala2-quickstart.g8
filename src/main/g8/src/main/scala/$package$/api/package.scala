package $package$

import zio.Has

package object api {
  type Api = Has[Api.Service]
}
