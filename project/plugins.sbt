addSbtPlugin("org.foundweekends.giter8" %% "sbt-giter8" % "0.12.0")
libraryDependencies += { "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value }
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.0")
