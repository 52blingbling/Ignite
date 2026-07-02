pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "EFA"

// 主应用入口
include(":app")

// 核心模块 (Core Modules)
include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:database")
include(":core:ai")

// 功能模块 (Feature Modules)
include(":feature:home")
include(":feature:focus")
include(":feature:analytics")
include(":feature:settings")
