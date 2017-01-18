# use-webjars-maven-plugin
Use webjars as if they would be npm modules.

# Why?
[Webjars](http://www.webjars.org/documentation) work well in the JVM world.
But as soon as a project comes into contact with the javascript / npm / node.js
eco system, things start to get complicated.
There are mainly two reasons for that:

- webjars basically are zip files. Therefore the contents is not directly available without unzipping.
- webjars encode their version in the directory structure. Therefore every tool processing them must configure somewhere the version number.

This plugin treats webjars the same way as npm does with its modules:

- unpack the webjar
- strip the version from its path

This way, tools like minifiers, css postprocessors, unit test runners etc. can be used with less hassle.

# Usage

The basic configuration looks like this:
````xml
<build>
    <plugins>
        <plugin>
            <groupId>guru.nidi.maven.plugins</groupId>
            <artifactId>use-webjars-maven-plugin</artifactId>
            <version>0.0.1</version>
            <executions>
                <execution>
                    <goals>
                        <goal>unpack</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>

<dependencies>
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>angularjs</artifactId>
        <version>1.5.7</version>
    </dependency>
</dependencies>
````

Running `mvn package` results in this file structure:
````
src
    ...
target
    webjars
        META-INF
            resources
                webjars
                    angularjs
                        ...
````

# Configuration
## Flatten
Adding 
````
<configuration>
    <flatten>true</flatten>
</configuration>
````
omits `META-INF/resources/webjars` and results in 
````
src
    ...
target
    webjars
        angularjs
            ...
````

## Further settings
The plugin is based on [`maven-dependency-plugin:unpack-dependencies`](https://maven.apache.org/plugins/maven-dependency-plugin/unpack-dependencies-mojo.html) 
and supports all its settings.