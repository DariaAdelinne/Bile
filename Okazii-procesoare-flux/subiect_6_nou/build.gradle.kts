plugins {
    kotlin("jvm") version "1.9.0" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        val implementation by configurations
        implementation(kotlin("stdlib"))
        implementation("io.reactivex.rxjava3:rxjava:3.0.0")
        implementation("io.reactivex.rxjava3:rxkotlin:3.0.0")
    }
}
