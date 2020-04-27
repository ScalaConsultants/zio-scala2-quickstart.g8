package $package$

import zio.Has

package object server {
  type HttpServer = Has[HttpServer.Service]
}
