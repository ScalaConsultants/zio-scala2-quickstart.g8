![Scala CI](https://github.com/ScalaConsultants/zio-akka-quickstart.g8/workflows/Scala%20CI/badge.svg)

## zio-scala2-quickstart

A [Giter8][g8] template for a fully functional, ready to deploy microservice (or monolith - it's up to you).

This template will result in Scala 2.13.x compatible code. For Scala 3 use [zio-scala3-quickstart][zio-scala3-quickstart].

Out of the box you get a set of CRUD endpoints in the framework of you choice (currently [ZIO HTTP][zio-http] or [Akka HTTP][akka-http]) that integrate with a PostgreSQL database (currently [Slick][slick] but more coming in the near future).

Other notable integrations include:
* `Testcontainers` for integration tests
* `Flyway migrations`
* `sbt-native-packager` for docker images
* `scalafmt`
* `zio-json` for JSON processing.
* `zio-logging` for logging.
* `zio-config` for typesafe configuration.
* `zio-test` for testing.

### Setting up the project

```bash
sbt new ScalaConsultants/zio-scala2-quickstart.g8
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
[zio-http]: https://zio.github.io/zio-http/
[slick]: https://scala-slick.org/
[zio-scala3-quickstart]: https://github.com/ScalaConsultants/zio-scala3-quickstart.g8
