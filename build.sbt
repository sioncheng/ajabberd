

name := "ajabberd"

scalaVersion := "2.11.8"

version := "1.0"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-agent" % "2.3.6",
    "com.typesafe.akka" %% "akka-actor" % "2.3.6",
    "com.typesafe.akka" %% "akka-remote" % "2.3.6",
    "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
    "org.scalatest" %% "scalatest" % "2.1.6" % "test"
)

// https://mvnrepository.com/artifact/xmlpull/xmlpull
//1.1.3.4d_b4_min
libraryDependencies += "xmlpull" % "xmlpull" % "1.1.3.1"
//
libraryDependencies += "net.sf.kxml" % "kxml2" % "2.2.2"


