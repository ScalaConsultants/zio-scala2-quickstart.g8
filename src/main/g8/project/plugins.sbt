addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.3")
$if(add_sbt_tpolecat.truthy)$
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.13")
$endif$
