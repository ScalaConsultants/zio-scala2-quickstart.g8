val akkaHttpVersion       = "10.2.9"
val akkaVersion           = "2.6.19"
val slickVersion          = "3.3.3"
val zioVersion            = "2.0.1"
val zioLoggingVersion     = "2.1.0"
val zioConfigVersion      = "3.0.2"
val flywayVersion         = "9.1.6"
val testContainersVersion = "0.40.10"
val postgresVersion       = "42.5.0"
val logbackClassicVersion = "1.2.11"
val zioSlickInterop       = "0.5.0"
val zioAkkaHttpInterop    = "0.6.0"
val zioJsonVersion        = "0.3.0-RC11"
val akkaHttpZioJson       = "1.40.0-RC3"

val dockerReleaseSettings = Seq(
  dockerExposedPorts   := Seq(8080),
  dockerExposedVolumes := Seq("/opt/docker/logs"),
  dockerBaseImage      := "eclipse-temurin:17.0.4_8-jre"
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
    name           := "$name$",
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka"  %% "akka-http"                       % akkaHttpVersion,
      "com.typesafe.akka"  %% "akka-actor-typed"                % akkaVersion,
      "com.typesafe.akka"  %% "akka-stream"                     % akkaVersion,
      "com.typesafe.slick" %% "slick"                           % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp"                  % slickVersion,
      "dev.zio"            %% "zio-json"                        % zioJsonVersion,
      "de.heikoseeberger"  %% "akka-http-zio-json"              % akkaHttpZioJson,
      "dev.zio"            %% "zio"                             % zioVersion,
      "dev.zio"            %% "zio-config"                      % zioConfigVersion,
      "dev.zio"            %% "zio-config-magnolia"             % zioConfigVersion,
      "dev.zio"            %% "zio-config-typesafe"             % zioConfigVersion,
      "io.scalac"          %% "zio-akka-http-interop"           % zioAkkaHttpInterop,
      "io.scalac"          %% "zio-slick-interop"               % zioSlickInterop,
      "ch.qos.logback"      % "logback-classic"                 % logbackClassicVersion,
      "dev.zio"            %% "zio-logging"                     % zioLoggingVersion,
      "dev.zio"            %% "zio-logging-slf4j"               % zioLoggingVersion,
      "org.postgresql"      % "postgresql"                      % postgresVersion,
      "org.flywaydb"        % "flyway-core"                     % flywayVersion,
      "com.typesafe.akka"  %% "akka-http-testkit"               % akkaHttpVersion       % Test,
      "com.typesafe.akka"  %% "akka-stream-testkit"             % akkaVersion           % Test,
      "com.typesafe.akka"  %% "akka-actor-testkit-typed"        % akkaVersion           % Test,
      "dev.zio"            %% "zio-test-sbt"                    % zioVersion            % Test,
      "com.dimafeng"       %% "testcontainers-scala-postgresql" % testContainersVersion % It
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    dockerReleaseSettings
  )
  .enablePlugins(DockerPlugin, JavaAppPackaging)
