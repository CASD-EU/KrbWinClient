# KrbWinClient

This java client can test your JDK 11+ native SSPI bridge to access kreberos ticket inside Windows LSA cache

## Prerequis

This project requires jdk 11+ and maven 3.9.x+

## Generate project

```powershell
mvn archetype:generate -DgroupId=eu.casd -DartifactId=krbReader \
    -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

## Build the project

```powershell
mvn clean compile
mvn test                  # runs the unit test (requires valid TGT)
mvn package               # creates target/krbReader-1.0-SNAPSHOT.jar.jar
mvn package assembly:single   # creates fat jar with dependencies
```

## run the application

```powershell
java -jar target/krbReader-1.0-SNAPSHOT.jar
# or with a service ticket request:
java -jar target/krbReader-1.0-SNAPSHOT.jar "HTTP/yourserver.domain.com"
```

If you want to add more java env var, you can use the below commands.

```cmd
java ^
  -Dsun.security.jgss.native=true ^
  -Djavax.security.auth.useSubjectCredsOnly=false ^
  -DSSPI_BRIDGE_TRACE=1 ^
  -jar target\krbReader-1.0-SNAPSHOT.jar
```

> The options `-D` must be put before -jar, and it only works on `cmd`. 
> You can't use it on `powershell`.