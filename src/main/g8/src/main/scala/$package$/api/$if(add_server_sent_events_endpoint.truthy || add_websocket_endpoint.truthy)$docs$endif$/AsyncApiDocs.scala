package $package$.api.docs

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object AsyncApiDocs {

  def routes: Route =
    (get & pathPrefix("api-docs")) {
      path("asyncapi.yml") {
        getFromResource("asyncapi.yml")
      } ~ pathPrefix("asyncapi") {
        (pathEndOrSingleSlash & redirectToTrailingSlashIfMissing(StatusCodes.TemporaryRedirect)) {
          getFromResource("asyncapi/index.html")
        } ~ getFromResourceDirectory("asyncapi")
      }
    }

}
