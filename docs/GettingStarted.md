# ElectionGuard-Kotlin-Multiplatform Getting Started

_last update 9/06/2023_

## Requirements

1. Clone the electionguard-kotlin (egk) repo

```
  cd devhome
  git clone https://github.com/danwallach/electionguard-kotlin-multiplatform.git
```

2. **Java 17+**. Install as needed, and make it your default Java when working with egk.

    _In general, we will use the latest version of the JVM with long-term-support (aka LTS). 
    This is the "language level" or bytecode version, along with the library API's, that our code assumes. 
    Since new Java version are backwards compatible, you can use any version of the JVM greater than or equal to 17._

3. **Gradle 8.3**. The correct version of gradle will be installed when you invoke a gradle command. 
   To do so explicitly, you can do:

```
  cd devhome/electionguard-kotlin-multiplatform
  ./gradlew wrapper --gradle-version=8.3 --distribution-type=bin
```

4. **Kotlin 1.9.10**. The correct version of kotlin will be installed when you invoke a gradle command.
   SImilarly, all needed library dependencies are also downloaded when you invoke a gradle command.

Alternatively, you can use the IntelliJ IDE (make sure you update to the latest version). 
Do steps 1 and 2 above. Then, in the IDE top menu: 
   1. use File / New / "Project from existing sources"
   2. in popup window, navigate to _devhome/electionguard-kotlin-multiplatform_ and select that directory
   3. "Import project from existing model" / Gradle

IntelliJ will create and populate an IntelliJ project with the electionguard-kotlin-multiplatform sources. There's
lots of online help for using IntelliJ. Recommended if you plan on doing a good amount of Java/Kotlin coding.

## Building the library

To build the complete library and run the standard tests:

```
  cd devhome/electionguard-kotlin-multiplatform
  ./gradlew build
```

To just do a clean build (no tests):

```
  cd devhome/electionguard-kotlin-multiplatform
  ./gradlew clean assemble
```

You should find that the library jar file is placed into:

`egklib/build/libs/egklib-jvm-2.0.0-SNAPSHOT.jar
`
## Using the egk library in your own jvm-based project

We do not yet have egk uploaded to Maven Central, which is the standard way to distribute JVM libraries.

However, you can add the library to your gradle application by pointing directly to the jar, along with all
of its dependencies, for example:

```
  dependencies {
    implementation(files("devhome/electionguard-kotlin-multiplatform/egklib/build/libs/egklib-jvm-2.0.0-SNAPSHOT.jar"))
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("pro.streem.pbandk:pbandk-runtime:0.14.2")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
    
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    ...
  }
```

## Building a library fat jar

Rather than having to specify all of egk's dependencies in your project's build (which will change all the time), 
you can build a "fat jar" that includes egk and its dependencies.

(This is not yet working in electionguard-kotlin-multiplatform. see egk-webapps to build it there)

```
  cd devhome/egk-webapps
  ./gradlew fatJar
```

You should find that the fat jar file is placed into:

`devhome/egk-webapps/egklib/build/libs/egklib-2.0.0-all.jar
`

You can put this jar into your build like

```
  dependencies {
    implementation(files("devhome/egk-webapps/egklib/build/libs/egklib-2.0.0-all.jar"))
     ...
  }
```

And you can also add it to your classpath to execute programs from the command line:

```
/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath devhome/egk-webapps/egklib/build/libs/egklib-2.0.0-all.jar \
    electionguard.verifier.RunVerifierKt \
    -in /path/to/election_record
```

Since the main class is _electionguard.verifier.RunVerifierKt_ you can also run the verifier as:

```
/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -jar devhome/egk-webapps/egklib/build/libs/egklib-2.0.0-all.jar \
    -in /path/to/election_record
```

/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -jar devhome/egk-webapps/egklib/build/libs/egklib-2.0.0-all.jar \
    -in /path/to/election_record
