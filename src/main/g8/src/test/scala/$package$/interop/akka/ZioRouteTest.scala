package $package$.interop.akka

import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.testkit.{ RouteTest, TestFrameworkInterface }
import zio.test.ZIOSpecDefault

trait ZioRouteTest extends ZIOSpecDefault with TestFrameworkInterface with RouteTest {

  def failTest(msg: String): Nothing         = throw new Exception(msg)
  def testExceptionHandler: ExceptionHandler = ExceptionHandler { case e: Throwable =>
    throw e
  }
}
