val ktor_version: String by project
val logback_version: String by project
val org_gradle_jvmargs: String by project
group = "com.github.masx200"
plugins {
    kotlin("jvm") version "2.0.21"
    id("io.ktor.plugin") version "3.0.1"
    id("org.graalvm.buildtools.native") version "0.9.19"
    id("maven-publish")
}
//distributions{
//    applicationDefaultJvmArgs
//}
application {
    mainClass.set("com.github.masx200.fast_and_mercury_router_reverse_proxy_server.ReverseProxyApplicationKt")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.ktor:ktor-server-cio-jvm")
    implementation("io.ktor:ktor-server-compression:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-netty-jvm")
    // https://mvnrepository.com/artifact/io.netty/netty-common
    implementation("io.netty:netty-common:4.1.114.Final")

    implementation("io.ktor:ktor-client-cio-jvm")
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
    implementation("io.ktor:ktor-client-encoding:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.apache.httpcomponents:httpcore:4.4.16")
}
tasks.named<JavaExec>("run") {
//    mainClass.set("com.example.Main")
//    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = (org_gradle_jvmargs.split(" "))
}

graalvmNative {
    binaries {

        named("main") {
            fallback.set(false)
            verbose.set(true)

            buildArgs.add("--initialize-at-build-time=ch.qos.logback")
            buildArgs.add("--initialize-at-build-time=io.ktor,kotlin")
            buildArgs.add("--initialize-at-build-time=org.slf4j.LoggerFactory")

            buildArgs.add("-H:+InstallExitHandlers")
            buildArgs.add("-H:+ReportUnsupportedElementsAtRuntime")
            buildArgs.add("-H:+ReportExceptionStackTraces")

            imageName.set("fast-and-mercury-router-reverse-proxy-server")
        }
    }
}