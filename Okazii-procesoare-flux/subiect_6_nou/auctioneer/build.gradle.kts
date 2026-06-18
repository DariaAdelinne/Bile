plugins {
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("AuctioneerMicroserviceKt")
}

dependencies {
    implementation(project(":message-library"))
}
