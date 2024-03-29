plugins {
    application
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
    id("org.jmailen.kotlinter") version "4.0.0"
    id("org.flywaydb.flyway") version "9.19.0"
    id("nu.studer.jooq") version "4.1"
}

group = "io.github.cfstout.keycloak-playground"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val dbUser by extra { "keycloak" }
val dbPw by extra { "password" }
val dbUrl by extra { "jdbc:postgresql://localhost:54321/keycloak" }

apply(from = "jooq.gradle")

flyway {
    url = dbUrl
    user = dbUser
    password = dbPw
    validateMigrationNaming = true
}

val deps by extra {
    mapOf(
        "commons-validator" to "1.8.0",
        "flyway" to "10.0.0",
        "hikari" to "3.4.2",
        "konfig" to "1.6.10.0",
        "h2" to "2.2.224",
        "jackson" to "2.15.3",
        "junit" to "5.6.2",
        "konfig" to "1.6.10.0",
        "ktor" to "1.6.8",
        "logback" to "1.4.11",
        "postgres" to "42.2.12",
        "slf4j" to "2.0.9"
    )
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("ch.qos.logback", "logback-classic", deps["logback"])
    implementation("com.natpryce", "konfig", deps["konfig"])
    implementation("commons-validator", "commons-validator", deps["commons-validator"])
    implementation("com.zaxxer", "HikariCP", deps["hikari"])
    implementation("io.ktor","ktor-freemarker", deps["ktor"])
    implementation("io.ktor", "ktor-server-netty", deps["ktor"])
    implementation("io.ktor", "ktor-serialization", deps["ktor"])
    implementation("org.jooq", "jooq")
    implementation("org.slf4j", "slf4j-api", deps["slf4j"])
    runtimeOnly("org.postgresql", "postgresql", deps["postgres"])

    jooqRuntime("org.postgresql", "postgresql", deps["postgres"])

    testImplementation("org.junit.jupiter", "junit-jupiter-api", deps["junit"])
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", deps["junit"])
    testImplementation("io.ktor", "ktor-server-tests", deps["ktor"])
    testImplementation("org.flywaydb", "flyway-core", deps["flyway"])
    testImplementation("org.flywaydb", "flyway-database-postgresql",deps["flyway"])
    testImplementation("com.h2database", "h2", deps["h2"])
}

tasks {
    (run) {
        args = listOf("config")
    }

    test {
        useJUnitPlatform()
    }
}

// DB setup
val startDockerCompose = tasks.register("startDockerCompose", Exec::class) {
    println(System.getenv("PATH"))

    commandLine("docker-compose", "up", "-d")
    doLast {
        println("Docker Compose up!")
    }
}

val waitForDatabase = tasks.register("waitForDatabase", Exec::class) {
    dependsOn(startDockerCompose)
    commandLine("sh", "-c", "until docker-compose exec -T postgres pg_isready ; do sleep 1; done")
    doLast {
        println("Database is ready!")
    }
}

tasks.named("generatePrimaryDbJooqSchemaSource").configure {
    dependsOn(waitForDatabase)
    dependsOn("flywayMigrate")
    dependsOn("formatKotlin")
    dependsOn("lintKotlin")
}

tasks.named("flywayMigrate").configure {
    dependsOn(waitForDatabase)
}

tasks.named("check") {
    dependsOn("formatKotlin")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("io.github.cfstout.keycloak.Server")
}
