package $package$.server

import $package$.api
import $package$.api.Api
$if(add_caliban_endpoint.truthy)$
import com.example.api.GraphQLApi
$endif$
import $package$.config.ApiConfig
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
$if(add_caliban_endpoint.truthy)$
import akka.http.scaladsl.server.RouteConcatenation._
$endif$
import zio._
import zio.config.Config

object HttpServer {

    trait Service {
      def start: Managed[Throwable, Http.ServerBinding]
    }

    def live: ZLayer[Has[ActorSystem] with Config[ApiConfig] with Api$if(add_caliban_endpoint.truthy)$ with GraphQLApi$endif$, Nothing, HttpServer] =
      ZLayer.fromServices[ActorSystem, ApiConfig, Api.Service$if(add_caliban_endpoint.truthy)$, api.graphql.GraphQLApi.Service$endif$, HttpServer.Service] { (sys, cfg, api$if(add_caliban_endpoint.truthy)$, gApi$endif$) =>
        new Service {
          implicit val system = sys
          val start =
            ZManaged.make(ZIO.fromFuture(_ => Http().bindAndHandle(api.routes$if(add_caliban_endpoint.truthy)$ ~ gApi.routes$endif$, cfg.host, cfg.port)))(b =>
              ZIO.fromFuture(_ => b.unbind()).orDie
            )
        }
      }

    def start: ZManaged[HttpServer, Throwable, Http.ServerBinding] =
      ZManaged.accessManaged[HttpServer](_.get.start)
  }
