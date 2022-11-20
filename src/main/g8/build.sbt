$if(enable_akka_http.truthy)$
val akkaHttpVersion       = "10.2.10"
val akkaVersion           = "2.6.20"
val zioAkkaHttpInterop    = "0.6.0"
val akkaHttpZioJson       = "1.40.0-RC3"
$endif$
$if(enable_zio_http.truthy)$
val zioHttpVersion        = "2.0.0-RC10"
$endif$
$if(enable_slick.truthy)$
val slickVersion          = "3.4.1"
val zioSlickInterop       = "0.5.0"
$endif$
$if(enable_quill.truthy)$
val quillVersion          = "4.6.0"
$endif$
val zioVersion            = "2.0.4"
val zioLoggingVersion     = "2.1.2"
val zioConfigVersion      = "3.0.2"
val flywayVersion         = "9.4.0"
val testContainersVersion = "0.40.11"
val postgresVersion       = "42.5.0"
val zioJsonVersion        = "0.3.0-RC11"
val logbackClassicVersion = "1.2.11"
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
$if(enable_zio_http.truthy)$
      // zio-http
      "io.d11" %% "zhttp" % zioHttpVersion,
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
      "org.postgresql"  % "postgresql"          % postgresVersion,
      "org.flywaydb"    % "flyway-core"         % flywayVersion,

      // logging
      "dev.zio"             %% "zio-logging"       % zioLoggingVersion,
      "dev.zio"             %% "zio-logging-slf4j" % zioLoggingVersion,
      "ch.qos.logback"       % "logback-classic"   % logbackClassicVersion,
      "org.fusesource.jansi" % "jansi"             % jansiVersion,

      // test
$if(enable_akka_http.truthy)$
      "com.typesafe.akka"  %% "akka-http-testkit"               % akkaHttpVersion       % Test,
      "com.typesafe.akka"  %% "akka-stream-testkit"             % akkaVersion           % Test,
      "com.typesafe.akka"  %% "akka-actor-testkit-typed"        % akkaVersion           % Test,
$endif$
      "dev.zio"            %% "zio-test-sbt"                    % zioVersion            % Test,
      "com.dimafeng"       %% "testcontainers-scala-postgresql" % testContainersVersion % It
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    dockerReleaseSettings
  )
  .enablePlugins(DockerPlugin, JavaAppPackaging)
