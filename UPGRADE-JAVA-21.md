Upgrade to Java 21 (LTS)

This repository has been prepared to compile against Java 21 by updating the Maven compiler settings.

What changed
- pom.xml: set maven.compiler.release to 21 and updated maven-compiler-plugin to 3.11.0

Local steps to complete the upgrade
1. Install JDK 21 (Adoptium / Temurin / Oracle)
   - Windows (chocolatey): choco install temurin-21-jdk
   - Manual: download from https://adoptium.net or https://www.oracle.com/java/technologies/downloads/

2. Ensure JAVA_HOME points to the JDK 21 installation and update PATH.
   - Windows (cmd.exe):
     setx JAVA_HOME "C:\\Program Files\\Eclipse Adoptium\\jdk-21"
     setx PATH "%JAVA_HOME%\\bin;%PATH%"
   - After setting, open a new terminal and verify:
     java -version
     javac -version

3. Build the project with Maven
   mvn -U clean package

Notes
- If you use an IDE, configure the project SDK to Java 21.
- If compilation fails, check for usages of removed/renamed APIs between Java 8 and Java 21 and update dependencies accordingly.
- Consider running a static analysis tool or migration helpers if there are larger incompatibilities.
