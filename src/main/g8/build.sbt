val zioVersion            = "2.0.13"
val zioJsonVersion        = "0.3.0-RC11"
val zioConfigVersion      = "3.0.2"
val logbackClassicVersion = "1.2.11"
val postgresqlVersion     = "42.5.4"
val testContainersVersion = "0.40.11"
val zioLoggingVersion     = "2.1.2"
val flywayVersion         = "9.15.0"
$if(enable_akka_http.truthy)$
val akkaHttpVersion       = "10.2.10"
val akkaVersion           = "2.6.20"
val zioAkkaHttpInterop    = "0.6.0"
val akkaHttpZioJson       = "1.40.0-RC3"
$endif$
$if(enable_zio_http.truthy)$
val zioHttpVersion        = "3.0.0-RC1"
$endif$
$if(enable_slick.truthy)$
val slickVersion          = "3.4.1"
val zioSlickInterop       = "0.6.0"
$endif$
$if(enable_quill.truthy)$
val quillVersion          = "4.6.0"
$endif$

val dockerReleaseSettings = Seq(
  dockerExposedPorts   := Seq(8080),
  dockerExposedVolumes := Seq("/opt/docker/logs"),
  dockerBaseImage      := "eclipse-temurin:17.0.4_8-jre"
)

val root = (project in file("."))
  .settings(
    inThisBuild(
      List(
        organization := "$organization$",
        scalaVersion := "$scala_version$"
      )
    ),
    name           := "$name$",
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Seq(
$if(enable_zio_http.truthy)$
      // zio-http
      "dev.zio" %% "zio-http" % zioHttpVersion,
$endif$
$if(enable_akka_http.truthy)$
      // akka-http
      "com.typesafe.akka" %% "akka-http"             % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"      % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"           % akkaVersion,
      "de.heikoseeberger" %% "akka-http-zio-json"    % akkaHttpZioJson,
      "io.scalac"         %% "zio-akka-http-interop" % zioAkkaHttpInterop,
$endif$

$if(enable_slick.truthy)$
      // slick
      "com.typesafe.slick" %% "slick"             % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp"    % slickVersion,
      "io.scalac"          %% "zio-slick-interop" % zioSlickInterop,
$endif$
$if(enable_quill.truthy)$
      // quill
      "io.getquill" %% "quill-jdbc-zio" % quillVersion,
$endif$
      // general
      "dev.zio"        %% "zio-json"            % zioJsonVersion,
      "dev.zio"        %% "zio"                 % zioVersion,
      "dev.zio"        %% "zio-config"          % zioConfigVersion,
      "dev.zio"        %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio"        %% "zio-config-typesafe" % zioConfigVersion,
      "org.postgresql"  % "postgresql"          % postgresqlVersion,
      "org.flywaydb"    % "flyway-core"         % flywayVersion,

      // logging
      "dev.zio"             %% "zio-logging"       % zioLoggingVersion,
      "dev.zio"             %% "zio-logging-slf4j" % zioLoggingVersion,
      "ch.qos.logback"       % "logback-classic"   % logbackClassicVersion,

      // test
$if(enable_akka_http.truthy)$
      "com.typesafe.akka"  %% "akka-http-testkit"               % akkaHttpVersion       % Test,
      "com.typesafe.akka"  %% "akka-stream-testkit"             % akkaVersion           % Test,
      "com.typesafe.akka"  %% "akka-actor-testkit-typed"        % akkaVersion           % Test,
$endif$
      "dev.zio"            %% "zio-test-sbt"                    % zioVersion            % Test,
      "com.dimafeng"       %% "testcontainers-scala-postgresql" % testContainersVersion % Test
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    dockerReleaseSettings
  )
  .enablePlugins(DockerPlugin, JavaAppPackaging)
