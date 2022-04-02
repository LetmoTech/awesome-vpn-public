import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
    kotlin("plugin.jpa") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "org.kamikadzy"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    maven ( "https://jitpack.io")
    maven ("https://mvn.mchv.eu/repository/mchv/")

}

dependencies {
    implementation(project(":RisexAPI", "default"))
    implementation(project(":utils", "default"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("org.telegram:telegrambots:5.7.1")
    implementation("org.telegram:telegrambots-spring-boot-starter:5.7.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    implementation("com.google.code.gson:gson:2.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")


    implementation("com.github.binance-exchange:binance-java-api:master-SNAPSHOT")

    // import the BOM
    implementation( platform("it.tdlight:tdlight-java-bom:2.8.1.2"))

    // do not specify the versions on the dependencies below!
    implementation ("it.tdlight:tdlight-java")

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
