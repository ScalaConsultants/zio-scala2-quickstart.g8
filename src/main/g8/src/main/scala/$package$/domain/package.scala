package $package$

import zio.Has


package object domain {

  type ItemRepository = Has[ItemRepository.Service]
  type HealthCheck    = Has[HealthCheck.Service]
  $if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
  type Subscriber         = Has[Subscriber.Service]
  $endif$

}
