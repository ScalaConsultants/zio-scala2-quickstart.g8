![Scala CI](https://github.com/ScalaConsultants/zio-akka-quickstart.g8/workflows/Scala%20CI/badge.svg)

## zio-akka-quickstart

A [Giter8][g8] template for a fully functional, ready to deploy microservice (or monolith - it's up to you).

This template integrates [ZIO][zio] with [Akka HTTP][akka-http] and [Slick][slick], so that you can seamlessly use all the power of ZIO in a familiar environment of battle tested Akka HTTP routes and Slick query DSL. Out of the box there's a set of REST endpoints for example CRUD operations ready to refactor.

Other notable integrations include:
* (Optional) GraphQL endpoint with GraphQL console
* (Optional) WebSocket endpoint
* (Optional) SSE endpoint
* `Testcontainers` for integration tests
* `Flyway migrations`
* `sbt-native-packager` for docker images
* `scalafmt`
* `play-json` for JSON processing.
* `zio-logging` for logging.
* `zio-config` for typesafe configuration.
* `zio-test` for testing.

### Setting up the project

```bash
sbt new ScalaConsultants/zio-akka-quickstart.g8
# then follow interactive process to choose project name and other parameters
```

### Run tests

```bash
cd <project-name>
sbt test
```

### Launch and interact

```bash
sbt run

# create an item
curl --request POST \
  --url http://localhost:8080/items \
  --header 'content-type: application/json' \
  --data '{
	"name":"BigMac",
	"price": 10.0
}'

# get all items
curl --request GET \
  --url http://localhost:8080/items
```

Check out more routes in [Api.scala](https://github.com/ScalaConsultants/zio-akka-quickstart.g8/blob/master/src/main/g8/src/main/scala/%24package%24/api/Api.scala)

### Docker image

Template provides packaging as docker image out of the box, using [sbt-native-packager](https://sbt-native-packager.readthedocs.io/en/stable/) plugin.

To create an image, run:
```
sbt docker:publishLocal
```

This will create a docker image locally, named the same as your project is. You can run it like the following:

```
docker run -d -p 8080:8080 --name=<project_name> <project_name>:0.1.0-SNAPSHOT
```

### Main components

This sample app has several key components:

* `ItemRepository`, which is quite a regular slick repo, with the only twist - it works with `ZIO` and not `Future`s. Backed up by an H2 database and Hikari connection pool.
* `Api` - pretty standard akka-http CRUD endpoint, also powered by `ZIO`.
* `interop` - package, where all the integration magic is happening.
* `ApplicationService` - a more higher level service, that works with different error type and uses ZIO environment.
* `Boot` - wiring all the components together using `ZLayer`.
* `ApiSpec` - akka-http endpoint spec using zio-test.

### GraphQL

During initialization step, there is a possibility to add GraphQL endpoint managed by Caliban library. 
It adds two endpoints to the api: `/api/graphql` which is responsible for GraphQL queries and `/graphiql` with simple 
GraphiQL console to play with the API.

To try out this feature open the browser http://localhost:8080/graphiql

### What in the world is ZLayer?

If you're new to `ZIO` you might be confused about what's happening in `Boot.scala` and what is `ZLayer`, please check out ebook about this subject [Mastering ZLayer](TODO) as well as [the latest official documentation](https://zio.dev/docs/howto/howto_use_layers).

Template license
----------------
Written in 2020 by [Scalac Sp. z o.o.](https://scalac.io/?utm_source=scalac_github&utm_campaign=scalac1&utm_medium=web)

To the extent possible under law, the author(s) have dedicated all copyright and related
and neighboring rights to this template to the public domain worldwide.
This template is distributed without any warranty. See <http://creativecommons.org/publicdomain/zero/1.0/>.

[g8]: http://www.foundweekends.org/giter8/
[scalac]: https://scalac.io/
[zio]: https://zio.dev/
[akka-http]: https://doc.akka.io/docs/akka-http/current/index.html
[slick]: https://scala-slick.org/
[zlayer]: https://zio.dev/docs/howto/howto_use_layers#unleash-zio-environment-with-zlayer
[zmanaged]: https://zio.dev/docs/datatypes/datatypes_managed#managed-with-zio-environment
