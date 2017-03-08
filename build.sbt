

name := "ajabberd"

scalaVersion := "2.11.8"

version := "1.0"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-agent" % "2.4.12",
    "com.typesafe.akka" %% "akka-actor" % "2.4.12",
    "com.typesafe.akka" %% "akka-remote" % "2.4.12",
    "com.typesafe.akka" %% "akka-stream" % "2.4.12",
    "com.typesafe.akka" %% "akka-testkit" % "2.4.12" % "test",
    "org.scalatest" %% "scalatest" % "2.1.6" % "test"
)

// https://mvnrepository.com/artifact/xmlpull/xmlpull
//1.1.3.4d_b4_min
libraryDependencies += "xmlpull" % "xmlpull" % "1.1.3.1"
//
libraryDependencies += "net.sf.kxml" % "kxml2" % "2.2.2"
//
libraryDependencies += "com.fasterxml" % "aalto-xml" % "1.0.0"
//
//libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"
//
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7"
//
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
// https://mvnrepository.com/artifact/org.igniterealtime.smack/smack-core
libraryDependencies += "org.igniterealtime.smack" % "smack-core" % "4.1.9"
// https://mvnrepository.com/artifact/org.igniterealtime.smack/smack-tcp
libraryDependencies += "org.igniterealtime.smack" % "smack-tcp" % "4.1.9"
// https://mvnrepository.com/artifact/org.igniterealtime.smack/smack-java7
libraryDependencies += "org.igniterealtime.smack" % "smack-java7" % "4.1.9"







