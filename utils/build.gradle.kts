plugins {
    kotlin("jvm") version "1.6.10"
}

version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.nayuki:qrcodegen:1.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.json:json:20220320")
}