plugins {
	kotlin("jvm") version "2.4.20"
	kotlin("plugin.spring") version "2.4.20"
	kotlin("plugin.jpa") version "2.4.20"
	id("org.springframework.boot") version "4.0.10"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.itone.illya4gurenko"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-liquibase")
	implementation("org.springframework.security:spring-security-crypto")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")

	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
	testImplementation("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll(
			"-Xjsr305=strict",
			"-Xemit-jvm-type-annotations"
		)
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

sourceSets {
	create("integrationTest") {
		kotlin.srcDir("src/IntegrationTest/kotlin")
		resources.srcDir("src/IntegrationTest/resources")
		compileClasspath += sourceSets["main"].output + sourceSets["test"].output
		runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
	}
}

val integrationTestImplementation by configurations.getting {
	extendsFrom(configurations["testImplementation"])
}
val integrationTestRuntimeOnly by configurations.getting {
	extendsFrom(configurations["testRuntimeOnly"])
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<Test>("integrationTest") {
	description = "Runs integration tests."
	group = "verification"
	testClassesDirs = sourceSets["integrationTest"].output.classesDirs
	classpath = sourceSets["integrationTest"].runtimeClasspath
	useJUnitPlatform()
}

tasks.withType<ProcessResources> {
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
