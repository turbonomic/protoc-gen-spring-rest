# protoc-gen-spring-rest

This is a plugin built with [protoc-plugin-common](https://github.com/turbonomic/protoc-plugin-common) to generate Spring Web controllers for gRPC services.

## Usage
To use the plugin:

Since the plugin is not currently in Maven Central, you'll need to clone this repository and build (using ```mvn```) first.

Then, customize your services with http routes:

```
service EchoService {

    // The SingleEcho call makes a single request and gets a single response.
    // The "echo" in the request and response will be the same.
    rpc Echo(EchoRequest) returns (EchoResponse) {
        option (google.api.http) = {
            get: "/echo/{echo}"
        };
    }
}
```

Then configure your build to use the plugin. This is what that might look like with protobuf-maven-plugin:

```
<plugin>
  <groupId>org.xolstice.maven.plugins</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>${protobuf.maven.plugin.version}</version>
  <configuration>
    <protoSourceRoot>src/main/protobuf</protoSourceRoot>
    <protoTestSourceRoot>src/test/protobuf</protoTestSourceRoot>
    <!-- os.detected.classifier provided by os-maven-plugin -->
    <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
    <!-- grpc-java generates gRPC client and server stubs for protobuf-defined services -->
    <pluginId>grpc-java</pluginId>
    <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}</pluginArtifact>
    <protocPlugins>
      <protocPlugin>
        <id>spring-rest</id>
        <groupId>com.turbonomic</groupId>
        <artifactId>protoc-gen-spring-rest-plugin</artifactId>
        <version>HEAD-SNAPSHOT</version>
        <mainClass>com.turbonomic.protoc.spring.rest.Main</mainClass>
      </protocPlugin>
    </protocPlugins>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>compile</goal>
        <goal>compile-custom</goal>
        <goal>test-compile</goal>
        <goal>test-compile-custom</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Rebuild, and you should see a \*REST.proto class generated for each \*.proto class that you have. You can include the generated controllers in your application context with @ComponentScan or as explicit beans.

## Missing Features Shortlist
* Client streams not supported when customizing routes (supported when using default)
* Custom patterns not supported
* Variables not supported
* Not supporting * and ** in path.
