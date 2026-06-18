plugins {
    kotlin("jvm")
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