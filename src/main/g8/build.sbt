lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion     = "2.6.4"
lazy val slickVersion    = "3.3.2"
lazy val zioVersion      = "1.0.0-RC18-2"
$if(add_caliban_endpoint.truthy)$
lazy val calibanVersion  = "0.7.1"
$endif$

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "$organization$",
      scalaVersion := "2.13.1"
    )
  ),
  name := "$name$",
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  libraryDependencies ++= Seq(
    "com.typesafe.akka"     %% "akka-http"                   % akkaHttpVersion,
    "com.typesafe.akka"     %% "akka-http-spray-json"        % akkaHttpVersion,
    "com.typesafe.akka"     %% "akka-actor-typed"            % akkaVersion,
    "com.typesafe.akka"     %% "akka-stream"                 % akkaVersion,
    "com.typesafe.slick"    %% "slick"                       % slickVersion,
    "dev.zio"               %% "zio"                         % zioVersion,
    "dev.zio"               %% "zio-interop-reactivestreams" % "1.0.3.5-RC3",
    "ch.qos.logback"        % "logback-classic"              % "1.2.3",
    "com.h2database"        % "h2"                           % "1.4.200",
    $if(add_caliban_endpoint.truthy)$
    "com.github.ghostdogpr" %% "caliban"                     % calibanVersion,
    "com.github.ghostdogpr" %% "caliban-akka-http"           % calibanVersion,
    $endif$
    "com.typesafe.akka"     %% "akka-http-testkit"           % akkaHttpVersion % Test,
    "com.typesafe.akka"     %% "akka-actor-testkit-typed"    % akkaVersion % Test,
    "dev.zio"               %% "zio"                         % zioVersion % Test,
    "dev.zio"               %% "zio-test-sbt"                % zioVersion % Test
  ),
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
)
