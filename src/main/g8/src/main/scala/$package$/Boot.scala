package $package$

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import $package$.infrastructure._
import $package$.interop.slick.dbio._
import slick.driver.H2Driver.api._
import scala.io.StdIn
import zio.{ DefaultRuntime, ZIO }
import zio.blocking.Blocking
import $package$.infrastructure.tables.ItemsTable

object Boot extends App {

  val runtime = new DefaultRuntime() {}
  implicit val ec = runtime.platform.executor.asEC
  
  implicit val system = ActorSystem(name = "zio-example-system", defaultExecutionContext = Some(ec))

  class LiveEnv 
    extends SlickItemRepository 
    with LiveDatabaseProvider
    with Blocking.Live

  val liveEnv = new LiveEnv
  val items = TableQuery[ItemsTable.Items]

  val api = new Api(liveEnv)

  val setup = {
    import slick.jdbc.H2Profile.api._
    DBIO.seq(
      (items.schema).create
    )
  }

  val setupIO = ZIO.fromDBIO(setup).provide(liveEnv)
  runtime.unsafeRun(setupIO)
 
  val host = "0.0.0.0"
  val port = 8080

  val bindingFuture = Http().bindAndHandle(api.route, host, port)

  println(s"Server online at http://\$host:\$port/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
}
