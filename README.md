# AMBER
AMBER is an AI-enabled extension of JMH.

This repository contains: 
- `jmh-1.37`: the folder contained the extended version of the `jmh-core` module
- `jpt-service`: the dockerized Flask web application exposing the TSC models
- `jmh-core-1.37-all.jar`: the ready to use .jar file of AMBER

Please, consider that in order to use AMBER you have to run the docker service. In order to do that, follow the instructions in the `jpt-service` folder.

## AMBER Jar File Generation
To regenerate the AMBER jar file, the only step is to run in the `jmh-core` module of the `jmh-1.37` folder `mvn clean install`.

Once the `.jar` has been generated and the `jpt-service` is running, you can configure AMBER in your Java Projects. In the following, the instructions to setup AMBER in both Maven and Gradle Projects.

## Setup AMBER in Your Maven Projects
To include AMBER in your Maven project the first step is to put the `jmh-core-1.37-all.jar` in a folder inside your project, for example `libs/jmh-core-1.37-all.jar`. Then, run:
```
  mvn install:install-file -Dfile=/path/to/jmh-core-1.37-all.jar \
    -DgroupId=org.openjdk.jmh \
    -DartifactId=jmh-core \
    -Dversion=1.37-extended \
    -Dpackaging=jar
```

After this, you must modify the `pom.xml` file, adding
```
    <dependencies>
        <dependency>
            <groupId>org.openjdk.jmh</groupId>
            <artifactId>jmh-core</artifactId>
            <version>1.37-extended</version>
        </dependency>
        <dependency>
            <groupId>org.openjdk.jmh</groupId>
            <artifactId>jmh-generator-annprocess</artifactId>
            <version>1.37</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Plugin per JMH -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.openjdk.jmh</groupId>
                            <artifactId>jmh-generator-annprocess</artifactId>
                            <version>1.37</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>

            <!-- Plugin Maven Shade per creare un JAR eseguibile -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.4.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <createDependencyReducedPom>true</createDependencyReducedPom>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>org.openjdk.jmh.Main</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

Now, your benchmarks' classes can be developed also in the `src/main/java/` folder and you just have to reload the Maven project and run `mvn clean install`.
You are ready to run the microbenchmarks, and to have a look at all the possible options run `java -jar target/projectname-1.0-SNAPSHOT.jar -h`. The dynamic halt can be configured both in the command line options and annotations.

## Setup AMBER in Your Gradle Projects
If you are using a Gradle project and you want to use AMBER, the first step is to put the `jmh-core-1.37-all.jar` in a folder inside the project, for example `libs/jmh-core-1.37-all.jar`.
Then, in the `build.gradle` file, the following plugin and dependencies must be added:
```
    id("me.champeau.jmh") version "0.7.2"

    jmh files("libs/jmh-core-1.37-all.jar")
    jmhImplementation files("libs/jmh-core-1.37-all.jar")
```

Then reload the project. The JMH Gradle plugin needs for the benchmarks to be collocated in the `src/jmh/java` folder. Now, run the following commands in order:
```
    Clean
    Build (if not run in the past)
    jmhClasses
    jmhRunBytecodeGenerator
    jmhCompileGeneratedClasses
    jmhJar
```
You are ready to run the microbenchmarks, and to have a look at all the possible options run `java -cp "libs/jmh-core-1.37-all.jar:build/libs/rxjava-3.0.0-SNAPSHOT-jmh.jar" org.openjdk.jmh.Main -h`. The dynamic halt can be configured both in the command line options and annotations.

## Configuring the Dynamic Halt
Once the setup of AMBER is done, to configure the Dynamic Halt of your microbenchmarks you have two possibilities.
The first one consists in using the following annotation:
```
@DynamicHalt(model = "model_name")
```
The `model_name` could be one of `oscnn`, `fcn` or `rocket`. This annotation works both at method-level and class-level.

Otherwise, you can use the command line option:
```
-hmodel "model_name"
```

Please, consider that as default AMBER configure the connection with the dockerized `jpt_service` at host `localhost` and port `50001`. If, following the instruction of the `README.md` file in the `jpt_service` folder, you've decided to run the service on a different host or on a different port, you should explicitly tell AMBER the host or port you have chosen.
To do so, you can use the following annotation's parameters:
```
@DynamicHalt(model = "model_name",host="new_host",port="new_port")
```
Or the following command line options:
```
-hmodel "model_name" -host "new_host" -hport "new_port"