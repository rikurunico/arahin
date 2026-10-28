pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://developer.huawei.com/repo/")
    }
}
rootProject.name = "arahin"
include(":app")
