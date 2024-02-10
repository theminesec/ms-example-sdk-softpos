// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}

allprojects {
    configurations.all {
        resolutionStrategy {
            preferProjectModules()
            cacheChangingModulesFor(0, "seconds")
        }
    }
}
