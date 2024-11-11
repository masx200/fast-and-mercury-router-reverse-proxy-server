val ktor_version: String by project
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
    // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("io.ktor:ktor-server-compression:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-client-cio-jvm")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
    implementation("io.ktor:ktor-client-encoding:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("org.apache.httpcomponents:httpcore:4.4.15")
}
tasks.named<JavaExec>("run") {
//    mainClass.set("com.example.Main")
//    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = (org_gradle_jvmargs.split(" "))
}