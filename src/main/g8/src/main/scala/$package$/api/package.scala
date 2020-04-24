package $package$

import zio.Has

package object api {
  type Api = Has[Api.Service]
  $if(add_caliban_endpoint.truthy)$
  type GraphQLApi = Has[graphql.GraphQLApi.Service]
  $endif$
}
