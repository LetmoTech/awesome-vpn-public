/**
* JetBrains Space Automation
* This Kotlin-script file lets you automate build activities
* For more info, see https://www.jetbrains.com/help/space/automation.html
*/

job("Copy main repository") {
    git {
        depth = UNLIMITED_DEPTH
    }
}

job("Copy RisexAPI repository") {
    git("RisexAPI") {
        cloneDir = "awesome-vpn/RisexAPI"
        depth = UNLIMITED_DEPTH
    }
}

job("Build and run tests") {
   gradlew("openjdk:17", "build")
}