RHQ Agent Plugin Plugin
===

This is a Maven plugin to help building RHQ plugins.

Documentation
===

[Design documentation](https://docs.jboss.org/author/display/RHQ/Maven+plugin+for+RHQ+agent+plugin)

[Maven generated plugin documentation](http://tsegismont.github.com/rhq-agent-plugin-plugin/plugin-info.html)

Usage
===

Currently, this plugin is [published on bintray](https://bintray.com/repo/browse/tsegismont/rhq-maven-plugins)

Configure your Maven settings file to use this as a plugin repository:

http://dl.bintray.com/v1/content/tsegismont/rhq-maven-plugins

Sample POM
===

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.mycompany.rhq.agent.plugins</groupId>
    <artifactId>my-custom-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <!-- This project has the custom plugin packaging -->
    <packaging>rhq-agent-plugin</packaging>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    </properties>

    <dependencies>

        <!-- Dependencies provided by the plugin container -->

        <dependency>
            <groupId>org.rhq</groupId>
            <artifactId>rhq-core-domain</artifactId>
            <version>4.6.0</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.rhq</groupId>
            <artifactId>rhq-core-plugin-api</artifactId>
            <version>4.6.0</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.rhq</groupId>
            <artifactId>rhq-core-native-system</artifactId>
            <version>4.6.0</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
            <version>1.1.1</version>
            <scope>provided</scope>
        </dependency>

        <!-- Dependencies required by my plugin -->
        <!-- All dependencies under RUNTIME scope will be included in the plugin archive -->

        <dependency>
            <groupId>commons-httpclient</groupId>
            <artifactId>commons-httpclient</artifactId>
            <version>3.1</version>
            <exclusions>
                <exclusion>
                    <groupId>commons-logging</groupId>
                    <artifactId>commons-logging</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>commons-codec</groupId>
                    <artifactId>commons-codec</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>commons-codec</groupId>
            <artifactId>commons-codec</artifactId>
            <version>1.3</version>
        </dependency>

        <!-- Some test dependencies -->

        <dependency>
            <groupId>org.rhq</groupId>
            <artifactId>rhq-core-plugin-container</artifactId>
            <version>4.6.0</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.rhq</groupId>
            <artifactId>test-utils</artifactId>
            <version>4.6.0</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.rhq</groupId>
            <artifactId>rhq-core-plugin-container</artifactId>
            <version>4.6.0</version>
            <type>test-jar</type>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>

        <plugins>

            <!-- This is to get the Maven version available as a property  -->
            <!-- It will be used to customize the archive manifest file  -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <phase>initialize</phase>
                        <goals>
                            <goal>maven-version</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- Here comes the RHQ agent plugin plugin -->
            <plugin>
                <groupId>org.rhq.maven.plugins</groupId>
                <artifactId>rhq-agent-plugin-plugin</artifactId>
                <version>0.2</version>
                <!-- Tell Maven that this plugin will extend the standard lifecycle and packaging -->
                <!-- Without this the build fails to recognize the custom packaging -->
                <extensions>true</extensions>
                <configuration>
                    <!-- Here comes the project manifest customization  -->
                    <archive>
                        <manifest>
                            <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
                            <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                        </manifest>
                        <manifestEntries>
                            <Maven-Version>${maven.version}</Maven-Version>
                            <Java-Version>${java.version}</Java-Version>
                            <Java-Vendor>${java.vendor}</Java-Vendor>
                            <Os-Name>${os.name}</Os-Name>
                            <Os-Arch>${os.arch}</Os-Arch>
                            <Os-Version>${os.version}</Os-Version>
                            <Build-Number>${buildNumber}</Build-Number>
                            <Build-Time>${buildTime}</Build-Time>
                        </manifestEntries>
                    </archive>
                </configuration>
                <executions>
                    <!-- Here we configure the execution of optional mojos -->
                    <execution>
                        <id>deploy-to-dev-container</id>
                        <goals>
                            <goal>rhq-agent-plugin-deploy</goal>
                        </goals>
                        <phase>package</phase>
                        <configuration>
                            <deployDirectory>/path/to/dev/container/deploy/dir</deployDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

        </plugins>
    </build>
</project>
```





