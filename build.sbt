name := "chasqui"

version := "0.1-SNAPSHOT"
scalaVersion := "2.12.1"

fork in run := true
javaOptions += "-Xprof"

/*********************************************************************************************************************
  * Dependencies
  *******************************************************************************************************************/
libraryDependencies ++= {
  val scalaXmlV = "1.0.6"
  val akkaV = "2.4.17"
  val scalatestV = "3.0.1"
  val scalacOptions = Seq("-language:postfixOps")
  Seq(
    "com.typesafe.akka" %% "akka-actor"      % akkaV,
    "com.typesafe.akka" %% "akka-testkit"    % akkaV,
    "com.typesafe.akka" %% "akka-cluster"    % akkaV,
    "org.scalatest"     %% "scalatest"       % scalatestV % "test",
    "org.mockito"       % "mockito-core"          % "2.+"
    //"com.squants"  %% "squants"  % "0.5.3",
    //"com.storm-enroute" %% "scalameter" % "0.7",
  )
}