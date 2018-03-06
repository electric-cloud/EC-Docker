File repository = new File("/opt/electriccloud/electriccommander/repository/lib")


repository.eachFile { File f ->
    println f.toURL()
    this.class.classLoader.rootLoader.addURL(f.toURL())
}

String pack = "com.electriccloud.repo.client"
String argsClassName = pack + ".RetrieveArtifactVersionClientArguments"
String[] args = new String[3]
Class.forName(argsClassName).create(args)