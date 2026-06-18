plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.kapt") version "1.9.25"
    id("io.micronaut.application") version "4.4.4"
}

version = "1.0.0"
group = "com.sd.laborator"

dependencies {
    kapt("io.micronaut:micronaut-inject-java")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.25")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("com.rabbitmq:amqp-client:5.21.0")
    implementation("com.mysql:mysql-connector-j:9.0.0")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.yaml:snakeyaml")
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

application { mainClass.set("com.sd.laborator.ApplicationKt") }

java { sourceCompatibility = JavaVersion.toVersion("17") }

micronaut {
    version("4.6.3")
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.sd.laborator.*")
    }
}

tasks.withType<Test> { useJUnitPlatform() }
