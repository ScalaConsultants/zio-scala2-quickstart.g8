addSbtPlugin("org.foundweekends.giter8" %% "sbt-giter8" % "0.12.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")
libraryDependencies += { "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value }
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.3")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.13")
