package $package$.infrastructure.swagger

import akka.http.scaladsl.server.{ PathMatchers, Route }
import com.example.api.Api
import com.github.swagger.akka.{ SwaggerHttpService }
import com.github.swagger.akka.SwaggerHttpService.{ removeInitialSlashIfNecessary }
import com.github.swagger.akka.model.Info
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.model.StatusCodes

object SwaggerDocService extends SwaggerHttpService {
  val config    = ConfigFactory.load()
  val API_URL   = config.getString("swagger.api.url")
  val BASE_PATH = config.getString("swagger.api.base.path")
  val PROTOCOL  = config.getString("swagger.api.protocol")

  override val host     = API_URL
  override val basePath = BASE_PATH

  override def apiClasses: Set[Class[_]] = Set(classOf[Api])

  private[swagger] def apiDocsBase(path: String) = PathMatchers.separateOnSlashes(removeInitialSlashIfNecessary(path))

  override def routes: Route = {
    val base = apiDocsBase(apiDocsPath)
    path(base / "swagger.yaml") {
      get {
        getFromResource(s"api-docs/swagger.yaml")
      }
    } $if(add_swagger_ui.truthy)$, ~
    (get & pathPrefix("swagger")) {
      (pathEndOrSingleSlash & redirectToTrailingSlashIfMissing(StatusCodes.TemporaryRedirect)) {
        getFromResource("swagger/index.html")
      } ~ {
        getFromResourceDirectory("swagger")
      }
    } $endif$
  }

  override def info =
    new Info("Swagger  API - ZIO-Akka-quickstart", "1.0", "Swagger", "", None, None, Map.empty)

}
