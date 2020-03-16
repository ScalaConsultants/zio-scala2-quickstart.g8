package $package$

import zio.Has

package object domain {

  type ItemRepository = Has[ItemRepository.Service]
}
