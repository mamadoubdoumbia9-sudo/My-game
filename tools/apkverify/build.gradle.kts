plugins { java }
dependencies {
    implementation("com.android.tools.build:apksig:8.7.3")
}
tasks.register<Jar>("fatJar") {
    archiveFileName.set("apkverify.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
