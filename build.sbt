name := "skunk-scala-meetup"
scalaVersion := "3.5.2"

Compile / run / fork := true

val http4sVersion = "0.23.29"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-server" % http4sVersion,
  "org.tpolecat" %% "skunk-core" % "0.6.4",
  "org.slf4j" % "slf4j-nop" % "2.0.16"
)
