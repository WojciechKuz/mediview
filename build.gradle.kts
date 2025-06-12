import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    //maven("https://packages.jetbrains.team/maven/p/skija/maven")
    maven("https://repo.kotlin.link")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    //api("org.jetbrains.skija:${"skija-windows"}:${"0.93.6"}")
    //implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.12.0") // Yay! twelveMonkeys works 😄

    //implementation("org.jetbrains.kotlinx:multik-core:0.2.3")
    //implementation("org.jetbrains.kotlinx:multik-default:0.2.3")
    //api("space.kscience:kmath-core:0.3.1")
    //implementation("space.kscience:kmath-geometry:0.3.1")
    implementation("dev.romainguy:kotlin-math:1.6.0")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "mediview"
            packageVersion = "1.0.0"
        }
    }
}

tasks.withType<JavaExec>() {
    jvmArgs("-Xmx2048m") // did not work
}
tasks.test {
    jvmArgs("-Xmx2048m")
}
