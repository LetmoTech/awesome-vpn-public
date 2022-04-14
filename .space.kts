/**
* JetBrains Space Automation
* This Kotlin-script file lets you automate build activities
* For more info, see https://www.jetbrains.com/help/space/automation.html
*/

job("Build") {
    git {
        depth = UNLIMITED_DEPTH
    }
    
    container(displayName = "Build environment", image = "openjdk:17") {
        shellScript {
            interpreter = "/bin/bash"
            content = """
                    cd /mnt/space/work/awesome-vpn
                    microdnf install git
                    git submodule init
                    git submodule update --init --recursive
                    ./gradlew
                    gradle :build
                """
        }
    }
}