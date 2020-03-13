package $package$

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import $package$.infrastructure._
import $package$.interop.slick.dbio._
import slick.driver.H2Driver.api._
import scala.io.StdIn
import zio.{ Runtime, ZIO }
import $package$.infrastructure.tables.ItemsTable

object Boot extends App {

  implicit val system = ActorSystem("zio-example-system")
  implicit val ec = system.dispatcher

  class LiveEnv 
    extends SlickItemRepository 
    with LiveDatabaseProvider

  val liveEnv = new LiveEnv
  val items = TableQuery[ItemsTable.Items]

  val host = "0.0.0.0"
  val port = 8080

  val api = new Api(liveEnv, port)

  val setup = {
    import slick.jdbc.H2Profile.api._
    DBIO.seq(
      (items.schema).create
    )
  }

  val setupIO = ZIO.fromDBIO(setup).provide(liveEnv)
  Runtime.default.unsafeRun(setupIO)

  val bindingFuture = Http().bindAndHandle(api.route, host, port)

  println(s"Server online at http://\$host:\$port/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
}
