/**
* JetBrains Space Automation
* This Kotlin-script file lets you automate build activities
* For more info, see https://www.jetbrains.com/help/space/automation.html
*/

job("Build and run tests") {
   git {
       depth = UNLIMITED_DEPTH
   }
    
   git("RisexAPI") {
       cloneDir = "awesome-vpn/RisexAPI"
       depth = UNLIMITED_DEPTH
   }
   gradlew("openjdk:17", "build")
}