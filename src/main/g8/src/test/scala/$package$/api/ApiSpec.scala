package $package$.api

import zio.test.Assertion._
import zio.test._

// import akka.actor.testkit.typed.scaladsl.ActorTestKit
// import akka.http.scaladsl.marshalling.Marshal
// import akka.http.scaladsl.model._

object ApiSpec extends DefaultRunnableSpec {

  // lazy val testKit         = ActorTestKit()
  // implicit def typedSystem = testKit.system
  // override def createActorSystem(): akka.actor.ActorSystem =
    // testKit.system.toClassic

  // lazy val routes  = new UserRoutes(userRegistry).userRoutes

  // use the json formats to marshal and unmarshall objects in the test
  // import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  // import Api.JsonSupport._

  def spec =
    suite("Api")(test("it works") {
      assert(1)(equalTo(2))
    })
}
