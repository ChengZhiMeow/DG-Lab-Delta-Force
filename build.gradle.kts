// 插件
plugins {
    id("java") // Java
    kotlin("jvm") version "2.1.21" // Kotlin
    id("com.gradleup.shadow") version "8.3.3" // Shadowr
}

// 项目信息
group = "cn.chengzhiya"
version = "1.0.0"

// JDK版本
kotlin.jvmToolchain(21)
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven("https://maven.chengzhimeow.cn/releases")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.21")

    implementation("net.java.dev.jna:jna:5.17.0")
    implementation("net.java.dev.jna:jna-platform:5.17.0")

    implementation("org.yaml:snakeyaml:2.2")

    implementation("cn.chengzhiya:MHDF-HttpFramework-client:1.0.9")
}

// 任务配置
tasks {
    clean {
        delete("$rootDir/target")
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveFileName.set("${project.name}-${project.version}.jar")
        destinationDirectory.set(file("$rootDir/target"))
    }

    jar {
        manifest {
            attributes["Main-Class"] = "cn.chengzhiya.dglabdeltaforce.MainKt"
        }
    }
}