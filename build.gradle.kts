
val logback_version: String by project
val org_gradle_jvmargs: String by project

plugins {
    kotlin("jvm") version "2.0.20"
    id("io.ktor.plugin") version "3.0.1"
}
//distributions{
//    applicationDefaultJvmArgs
//}
application {
    mainClass.set("com.github.masx200.fast_and_mercury_router_reverse_proxy_server.ReverseProxyApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-html-builder")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-client-cio-jvm")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("org.jsoup:jsoup:1.16.1")
}
tasks.named<JavaExec>("run") {
//    mainClass.set("com.example.Main")
//    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = (org_gradle_jvmargs.split(" "))
}