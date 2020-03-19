package $package$

import akka.actor
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import $package$.api._
import $package$.api.Api.ApiConfig
import $package$.infrastructure._
import $package$.interop.slick.DatabaseProvider
import zio.console._
import zio.{ App, Has, ZIO, ZLayer, ZManaged }
import akka.http.scaladsl.server.RouteConcatenation.concat

object Boot extends App {

  val host = "0.0.0.0"
  val port = 8080

  val program: ZIO[Console with Api $if(add_caliban_endpoint.truthy)$with GraphQLApi $endif$with Has[ActorSystem], Throwable, Unit] = ZIO.effect {
    for {
      implicit0(system: ActorSystem) <- ZIO.access[Has[ActorSystem]](_.get[actor.ActorSystem])
      api                            <- ZIO.access[Api](_.get)
      $if(add_caliban_endpoint.truthy)$
      graphQLApi                     <- ZIO.access[GraphQLApi](_.get)
      $endif$
      routes                         = $if(add_caliban_endpoint.truthy)$concat(api.routes, graphQLApi.routes)$else$api.routes$endif$
      binding                        <- ZIO.fromFuture(_ => Http().bindAndHandle(routes, host, port))
      _                              <- putStrLn(s"Server online at http://\$host:\$port/\nPress RETURN to stop...")
      _                              <- getStrLn
      _                              <- ZIO.fromFuture(_ => binding.unbind())
    } yield ()
  }.flatten

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {

    val actorSystem = ZLayer.fromManaged(
      ZManaged.make(ZIO.effect(ActorSystem("zio-example-system")))(s => ZIO.fromFuture(_ => s.terminate()).either)
    )
    val dbLayer    = DatabaseProvider.live >>> SlickItemRepository.live
    val api        = (ZLayer.succeed(ApiConfig(port)) ++ dbLayer) >>> Api.live
    $if(add_caliban_endpoint.truthy)$
    val graphQLApi = (dbLayer ++ actorSystem) >>> GraphQLApi.live
    $endif$
    val liveEnv    = actorSystem ++ Console.live ++ api $if(add_caliban_endpoint.truthy)$++ graphQLApi$endif$

    program.provideLayer(liveEnv).fold(_ => 1, _ => 0)
  }

}
