// Dependencies are needed for Scala Steward to check if there are newer versions
val akkaHttpVersion       = "10.2.10"
val akkaVersion           = "2.6.20"
val zioAkkaHttpInterop    = "0.6.0"
val akkaHttpZioJson       = "1.40.0-RC3"
val zioHttpVersion        = "2.0.0-RC10"
val slickVersion          = "3.4.1"
val zioSlickInterop       = "0.5.0"
val quillVersion          = "4.6.0"
val zioVersion            = "2.0.2"
val zioLoggingVersion     = "2.1.2"
val zioConfigVersion      = "3.0.2"
val flywayVersion         = "9.4.0"
val testContainersVersion = "0.40.12"
val postgresVersion       = "42.5.0"
val zioJsonVersion        = "0.3.0-RC11"
val logbackClassicVersion = "1.2.11"
val jansiVersion          = "2.4.0"

lazy val It = config("it").extend(Test)

val root = (project in file("."))
  .enablePlugins(ScriptedPlugin)
  .configs(It)
  .settings(
    name           := "zio-akka-quickstart",
    Test / test    := {
      val _ = (Test / g8Test).toTask("").value
    },
    scriptedLaunchOpts ++= List(
      "-Xms1024m",
      "-Xmx1024m",
      "-XX:ReservedCodeCacheSize=128m",
      "-Xss2m",
      "-Dfile.encoding=UTF-8"
    ),
    resolvers += Resolver.url(
      "typesafe",
      url("https://repo.typesafe.com/typesafe/ivy-releases/")
    )(Resolver.ivyStylePatterns),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Seq(
      // zio-http
      "io.d11" %% "zhttp" % zioHttpVersion,

      // akka-http
      "com.typesafe.akka" %% "akka-http"             % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"      % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"           % akkaVersion,
      "de.heikoseeberger" %% "akka-http-zio-json"    % akkaHttpZioJson,
      "io.scalac"         %% "zio-akka-http-interop" % zioAkkaHttpInterop,

      // slick
      "com.typesafe.slick" %% "slick"             % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp"    % slickVersion,
      "io.scalac"          %% "zio-slick-interop" % zioSlickInterop,

      // quill
      "io.getquill" %% "quill-jdbc-zio" % quillVersion,

      // general
      "dev.zio"       %% "zio-json"            % zioJsonVersion,
      "dev.zio"       %% "zio"                 % zioVersion,
      "dev.zio"       %% "zio-config"          % zioConfigVersion,
      "dev.zio"       %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio"       %% "zio-config-typesafe" % zioConfigVersion,
      "org.postgresql" % "postgresql"          % postgresVersion,
      "org.flywaydb"   % "flyway-core"         % flywayVersion,

      // logging
      "dev.zio"             %% "zio-logging"       % zioLoggingVersion,
      "dev.zio"             %% "zio-logging-slf4j" % zioLoggingVersion,
      "ch.qos.logback"       % "logback-classic"   % logbackClassicVersion,
      "org.fusesource.jansi" % "jansi"             % jansiVersion,

      // test
      "com.typesafe.akka" %% "akka-http-testkit"               % akkaHttpVersion       % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"             % akkaVersion           % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed"        % akkaVersion           % Test,
      "dev.zio"           %% "zio-test-sbt"                    % zioVersion            % Test,
      "com.dimafeng"      %% "testcontainers-scala-postgresql" % testContainersVersion % It
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
