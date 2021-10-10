val akkaHttpVersion       = "10.2.6"
val akkaVersion           = "2.6.15"
val slickVersion          = "3.3.3"
val zioVersion            = "1.0.10"
val zioLoggingVersion     = "0.5.11"
val zioConfigVersion      = "1.0.6"
val zioRSVersion          = "1.3.5"
val flywayVersion         = "7.14.0"
val testContainersVersion = "0.39.5"
val postgresVersion       = "42.2.23"
val logbackClassicVersion = "1.2.5"
val zioSlickInterop       = "0.4"
val zioAkkaHttpInterop    = "0.5.0"
val zioJsonVersion        = "0.1.5"
val akkaHttpZioJson       = "1.37.0"
$if(add_caliban_endpoint.truthy)$
val calibanVersion        = "1.2.0"
$endif$

val dockerReleaseSettings = Seq(
  dockerExposedPorts := Seq(8080),
  dockerExposedVolumes := Seq("/opt/docker/logs"),
  dockerBaseImage := "adoptopenjdk/openjdk12:x86_64-ubuntu-jre-12.0.2_10"
)

lazy val It = config("it").extend(Test)

val root = (project in file("."))
  .configs(It)
  .settings(
    inConfig(It)(Defaults.testSettings),
    inThisBuild(
      List(
        organization := "$organization$",
        scalaVersion := "$scala_version$"
      )
    ),
    name := "$name$",
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka"     %% "akka-http"                       % akkaHttpVersion,
      "com.typesafe.akka"     %% "akka-actor-typed"                % akkaVersion,
      "com.typesafe.akka"     %% "akka-stream"                     % akkaVersion,
      "com.typesafe.slick"    %% "slick"                           % slickVersion,
      "com.typesafe.slick"    %% "slick-hikaricp"                  % slickVersion,
      "dev.zio"               %% "zio-json"                        % zioJsonVersion,
      "de.heikoseeberger"     %% "akka-http-zio-json"              % akkaHttpZioJson,
      "dev.zio"               %% "zio"                             % zioVersion,
      "dev.zio"               %% "zio-streams"                     % zioVersion,
      "dev.zio"               %% "zio-config"                      % zioConfigVersion,
      "dev.zio"               %% "zio-config-magnolia"             % zioConfigVersion,
      "dev.zio"               %% "zio-config-typesafe"             % zioConfigVersion,
      "io.scalac"             %% "zio-akka-http-interop"           % zioAkkaHttpInterop,
      "io.scalac"             %% "zio-slick-interop"               % zioSlickInterop,
      "dev.zio"               %% "zio-interop-reactivestreams"     % zioRSVersion,
      "ch.qos.logback"        % "logback-classic"                  % logbackClassicVersion,
      "dev.zio"               %% "zio-logging"                     % zioLoggingVersion,
      "dev.zio"               %% "zio-logging-slf4j"               % zioLoggingVersion,
      "org.postgresql"        % "postgresql"                       % postgresVersion,
      "org.flywaydb"          % "flyway-core"                      % flywayVersion,
      $if(add_caliban_endpoint.truthy)$
      "com.github.ghostdogpr" %% "caliban"                         % calibanVersion,
      "com.github.ghostdogpr" %% "caliban-akka-http"               % calibanVersion,
      $endif$
      "com.typesafe.akka"     %% "akka-http-testkit"               % akkaHttpVersion % Test,
      "com.typesafe.akka"     %% "akka-stream-testkit"             % akkaVersion % Test,
      "com.typesafe.akka"     %% "akka-actor-testkit-typed"        % akkaVersion % Test,
      "dev.zio"               %% "zio-test-sbt"                    % zioVersion % Test,
      "com.dimafeng"          %% "testcontainers-scala-postgresql" % testContainersVersion % It
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    dockerReleaseSettings
  )
  .enablePlugins(DockerPlugin, JavaAppPackaging)
