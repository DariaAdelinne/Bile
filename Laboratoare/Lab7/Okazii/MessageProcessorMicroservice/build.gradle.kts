plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":MessageLibrary"))

    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
}

kotlin {
    jvmToolchain(8)

    compilerOptions {
        languageVersion.set(
            org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3
        )
        apiVersion.set(
            org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3
        )
    }
}

sourceSets {
    main {
        kotlin.srcDirs("src")
    }
}

application {
    mainClass.set("MessageProcessorMicroserviceKt")
}