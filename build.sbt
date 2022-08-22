// Dependencies are needed for Scala Steward to check if there are newer versions
val akkaHttpVersion       = "10.2.9"
val akkaVersion           = "2.6.19"
val slickVersion          = "3.3.3"
val zioVersion            = "1.0.16"
val zioLoggingVersion     = "0.5.14"
val zioConfigVersion      = "2.0.5"
val flywayVersion         = "9.1.3"
val testContainersVersion = "0.40.10"
val postgresVersion       = "42.3.6"
val logbackClassicVersion = "1.2.11"
val zioSlickInterop       = "0.5.0"
val zioAkkaHttpInterop    = "0.5.0"
val zioJsonVersion        = "0.1.5"
val akkaHttpZioJson       = "1.39.2"

lazy val It = config("it").extend(Test)

val root = (project in file("."))
  .enablePlugins(ScriptedPlugin)
  .configs(It)
  .settings(
    name := "zio-akka-quickstart",
    Test / test := {
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
      "com.typesafe.akka"     %% "akka-http"                       % akkaHttpVersion,
      "com.typesafe.akka"     %% "akka-actor-typed"                % akkaVersion,
      "com.typesafe.akka"     %% "akka-stream"                     % akkaVersion,
      "com.typesafe.slick"    %% "slick"                           % slickVersion,
      "com.typesafe.slick"    %% "slick-hikaricp"                  % slickVersion,
      "dev.zio"               %% "zio-json"                        % zioJsonVersion,
      "de.heikoseeberger"     %% "akka-http-zio-json"              % akkaHttpZioJson,
      "dev.zio"               %% "zio"                             % zioVersion,
      "dev.zio"               %% "zio-config"                      % zioConfigVersion,
      "dev.zio"               %% "zio-config-magnolia"             % zioConfigVersion,
      "dev.zio"               %% "zio-config-typesafe"             % zioConfigVersion,
      "io.scalac"             %% "zio-akka-http-interop"           % zioAkkaHttpInterop,
      "io.scalac"             %% "zio-slick-interop"               % zioSlickInterop,
      "ch.qos.logback"         % "logback-classic"                 % logbackClassicVersion,
      "dev.zio"               %% "zio-logging"                     % zioLoggingVersion,
      "dev.zio"               %% "zio-logging-slf4j"               % zioLoggingVersion,
      "org.postgresql"         % "postgresql"                      % postgresVersion,
      "org.flywaydb"           % "flyway-core"                     % flywayVersion,
      "com.typesafe.akka"     %% "akka-http-testkit"               % akkaHttpVersion       % Test,
      "com.typesafe.akka"     %% "akka-stream-testkit"             % akkaVersion           % Test,
      "com.typesafe.akka"     %% "akka-actor-testkit-typed"        % akkaVersion           % Test,
      "dev.zio"               %% "zio-test-sbt"                    % zioVersion            % Test,
      "com.dimafeng"          %% "testcontainers-scala-postgresql" % testContainersVersion % It
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
