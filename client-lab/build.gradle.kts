plugins {
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(libs.lettuce.core)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass = "lab.redis.LabApplication"
    applicationDefaultJvmArgs = listOf(
        "-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
        "-Dio.netty.noUnsafe=true"
    )
}

tasks.test {
    useJUnitPlatform()
}

dependencyLocking {
    lockAllConfigurations()
}
