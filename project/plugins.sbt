addSbtPlugin("org.foundweekends.giter8" %% "sbt-giter8"          % "0.13.1")
addSbtPlugin("org.scalameta"             % "sbt-scalafmt"        % "2.4.3")
libraryDependencies += "org.scala-sbt"  %% "scripted-plugin"     % sbtVersion.value
addSbtPlugin("com.github.sbt"          % "sbt-native-packager" % "1.9.0")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"        % "0.1.20")
