resolvers += "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository"

addSbtPlugin("org.riot-framework" % "sbt-riotctl" % "0.7.1")
