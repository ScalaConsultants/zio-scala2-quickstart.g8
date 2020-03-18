lazy val akkaHttpVersion = "$akka_http_version$"
lazy val akkaVersion     = "$akka_version$"
lazy val slickVersion    = "$slick_version$"
lazy val zioVersion      = "$zio_version$"

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "$organization$",
      scalaVersion := "$scala_version$"
    )
  ),
  name := "$name$",
  libraryDependencies ++= Seq(
    "com.typesafe.akka"  %% "akka-http"                   % akkaHttpVersion,
    "com.typesafe.akka"  %% "akka-http-spray-json"        % akkaHttpVersion,
    "com.typesafe.akka"  %% "akka-actor-typed"            % akkaVersion,
    "com.typesafe.akka"  %% "akka-stream"                 % akkaVersion,
    "com.typesafe.slick" %% "slick"                       % slickVersion,
    "dev.zio"            %% "zio"                         % zioVersion,
    "dev.zio"            %% "zio-interop-reactivestreams" % "1.0.3.5-RC3",
    "ch.qos.logback"     % "logback-classic"              % "1.2.3",
    "com.h2database"     % "h2"                           % "1.4.200",
    "com.typesafe.akka"  %% "akka-http-testkit"           % akkaHttpVersion % Test,
    "com.typesafe.akka"  %% "akka-actor-testkit-typed"    % akkaVersion % Test,
    "dev.zio"            %% "zio-test-sbt"                % zioVersion % Test
  ),
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
)
