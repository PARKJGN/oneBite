// oneBite 백엔드 — Kotlin + Spring Boot (실용적 헥사고날) (T002)
plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "app"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

repositories { mavenCentral() }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail") // SMTP 발송(onebite.email=smtp)
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.rometools:rome:2.1.0") // RSS/Atom 파싱(FR-003a)
    implementation("com.google.firebase:firebase-admin:9.4.1") // FCM 푸시 전송(onebite.push=fcm)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")           // JWT(access) HS256
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("com.icegreen:greenmail-junit5:2.1.0") // 인프로세스 SMTP(메일 발송 검증)
}

dependencyManagement {
    imports { mavenBom("org.testcontainers:testcontainers-bom:1.20.3") }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions { freeCompilerArgs.add("-Xjsr305=strict") }
}

tasks.withType<Test> { useJUnitPlatform() }
