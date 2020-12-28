package $package$

import zio.Has

package object application {
  type ApplicationService = Has[ApplicationService.Service]
}
