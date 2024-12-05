# AMBER
AMBER is an AI-enabled extension of JMH.

This repository contains: 
- `jmh-1.37:` the folder contained the extended version of the `jmh-core` module
- `jpt-service`: the dockerized Flask web application exposing the TSC models
- `jmh-core-1.37-all.jar`: the ready to use .jar file of AMBER

## Configuring AMBER
To regenerate the AMBER jar file, the only step is to run `mvn clean install` in the `jmh-core folder`.

## Maven
If the user is using a Maven project the first step is to put the `jmh-core-1.37-all.jar` in a folder inside the project, for example `libs/jmh-core-1.37-all.jar`. Then, run:
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
