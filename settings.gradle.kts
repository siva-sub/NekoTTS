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
    
    resolutionStrategy {
        eachPlugin {
            when (requested.id.toString()) {
                "com.android.application" -> useVersion("8.5.2")
                "com.android.library" -> useVersion("8.5.2")
                "org.jetbrains.kotlin.android" -> useVersion("1.9.20")
                "org.jetbrains.kotlin.kapt" -> useVersion("1.9.20")
                "com.google.dagger.hilt.android" -> useVersion("2.48")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NekoTTS"
include(":app")