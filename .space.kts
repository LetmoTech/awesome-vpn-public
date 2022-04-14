/**
* JetBrains Space Automation
* This Kotlin-script file lets you automate build activities
* For more info, see https://www.jetbrains.com/help/space/automation.html
*/

job("Build") {
    git {
        depth = UNLIMITED_DEPTH
    }
    
    container(displayName = "Build environment", image = "ubuntu") {
        shellScript {
            interpreter = "/bin/bash"
            content = """
                    cd /mnt/space/work/awesome-vpn
                    sudo apt-get install git openjdk-17-jdk -y
                    git submodule --init --recursive
                    ./gradlew build
                """
        }
    }
}