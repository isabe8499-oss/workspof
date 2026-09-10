pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WorkSpoof"

include(
    ":app",
    ":core:data",
    ":core:policy",
    ":core:topology",
    ":feature:advanced",
    ":privileged:shizuku",
    ":xposed-stubs",
)
