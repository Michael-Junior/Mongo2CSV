ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "Mongo2CSV"
  )

val mongoVersion = "4.7.2"
val commonsCVSVersion = "1.9.0"

libraryDependencies ++= Seq(
  "org.mongodb.scala" %% "mongo-scala-driver" % mongoVersion,
  "org.apache.commons" % "commons-csv" % commonsCVSVersion,
)
