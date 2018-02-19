lazy val akkaHttpVersion = "10.1.0-RC2"
lazy val akkaVersion    = "2.5.9"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.4",
      name := "Akka CRUD with JPA"
    )),
    name := "akka-sample-crud-jpa",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "com.typesafe.akka" %% "akka-http-jackson"    % akkaHttpVersion,

      "org.hibernate" % "hibernate-core" % "5.2.13.Final",
      "hsqldb" % "hsqldb" % "1.8.0.10",

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "junit"              % "junit"             % "4.12"          % Test,
      "com.novocode"       % "junit-interface"   % "0.10"          % Test
    ),
    mainClass := Some("com.lightbend.akka.jpa.sample.QuickstartServer"),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
  )
