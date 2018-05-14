import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.2.41"
}

application {
    mainClassName = "xyz.teufelsgraben.recrypt.AppKt"
}

dependencies {
    compile(kotlin("stdlib"))
}

repositories {
    jcenter()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Test> {
        testLogging.showStandardStreams = true
    }

    withType<Jar> {
        manifest {
            attributes["Main-Class"] = application.mainClassName
        }
        from(configurations.runtime.map { if (it.isDirectory) it else zipTree(it) })
    }
}