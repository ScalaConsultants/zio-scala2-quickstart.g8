$if(enable_akka_http.truthy)$
val akkaHttpVersion       = "10.2.10"
val akkaVersion           = "2.6.20"
$endif$
val slickVersion          = "3.4.0"
val zioVersion            = "2.0.2"
val zioLoggingVersion     = "2.1.0"
val zioConfigVersion      = "3.0.2"
val flywayVersion         = "9.3.0"
val testContainersVersion = "0.40.10"
val postgresVersion       = "42.5.0"
val logbackClassicVersion = "1.4.4"
val zioSlickInterop       = "0.5.0"
$if(enable_akka_http.truthy)$
val zioAkkaHttpInterop    = "0.6.0"
$endif$
val zioJsonVersion        = "0.3.0-RC11"
$if(enable_akka_http.truthy)$
val akkaHttpZioJson       = "1.40.0-RC3"
$endif$
$if(enable_zio_http.truthy)$
val zioHttpVersion        = "2.0.0-RC10"
$endif$
val jansiVersion          = "2.4.0"

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
      $if(enable_zio_http.truthy)$"io.d11"             %% "zhttp"                           % zioHttpVersion,$endif$
      $if(enable_akka_http.truthy)$"com.typesafe.akka"  %% "akka-http"                       % akkaHttpVersion,$endif$
      $if(enable_akka_http.truthy)$"com.typesafe.akka"  %% "akka-actor-typed"                % akkaVersion,$endif$
      $if(enable_akka_http.truthy)$"com.typesafe.akka"  %% "akka-stream"                     % akkaVersion,$endif$
      "com.typesafe.slick" %% "slick"                           % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp"                  % slickVersion,
      "dev.zio"            %% "zio-json"                        % zioJsonVersion,
      $if(enable_akka_http.truthy)$"de.heikoseeberger"  %% "akka-http-zio-json"              % akkaHttpZioJson,$endif$
      "dev.zio"            %% "zio"                             % zioVersion,
      "dev.zio"            %% "zio-config"                      % zioConfigVersion,
      "dev.zio"            %% "zio-config-magnolia"             % zioConfigVersion,
      "dev.zio"            %% "zio-config-typesafe"             % zioConfigVersion,
      $if(enable_akka_http.truthy)$"io.scalac"          %% "zio-akka-http-interop"           % zioAkkaHttpInterop,$endif$
      "io.scalac"          %% "zio-slick-interop"               % zioSlickInterop,
      "ch.qos.logback"      % "logback-classic"                 % logbackClassicVersion,
      "dev.zio"            %% "zio-logging"                     % zioLoggingVersion,
      "dev.zio"            %% "zio-logging-slf4j"               % zioLoggingVersion,
      "org.postgresql"      % "postgresql"                      % postgresVersion,
      "org.flywaydb"        % "flyway-core"                     % flywayVersion,
      $if(enable_akka_http.truthy)$"com.typesafe.akka"  %% "akka-http-testkit"               % akkaHttpVersion       % Test,$endif$
      $if(enable_akka_http.truthy)$"com.typesafe.akka"  %% "akka-stream-testkit"             % akkaVersion           % Test,$endif$
      $if(enable_akka_http.truthy)$"com.typesafe.akka"  %% "akka-actor-testkit-typed"        % akkaVersion           % Test,$endif$
      "dev.zio"            %% "zio-test-sbt"                    % zioVersion            % Test,
      "com.dimafeng"       %% "testcontainers-scala-postgresql" % testContainersVersion % It,

      // jansi
      "org.fusesource.jansi" % "jansi" % jansiVersion
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    dockerReleaseSettings
  )
  .enablePlugins(DockerPlugin, JavaAppPackaging)
