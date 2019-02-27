# LightBox

LightBox is a simple configuration loader for Sponge plugins.

### Features

LightBox abstracts away the standard loading routine, allowing a
configuration file to be loaded in a single call.

LightBox also provides transparent support for default configurations placed
on the classpath, automatically updating configuration files on disk with any
missing values on load.

### Usage

First, add LightBox to your project:

##### Gradle

```groovy
repositories {
    maven {
        name 'caseif'
        url 'https://repo.caseif.net/content/groups/public/'
    }
}
dependencies {
    compile 'net.caseif.lightbox:lightbox:1.0.0'
}
```

##### Maven

```xml
<repositories>
    <repository>
        <id>caseif</id>
        <url>https://repo.caseif.net/content/groups/public/</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>net.caseif.lightbox</groupId>
        <artifactId>lightbox</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

Optionally, create a default configuration on your classpath. If your plugin has
the ID `foo`, then configuration `bar/quux.conf` should be placed at
`foo/bar/quux.conf`
in the root of your plugin JAR. In both Gradle and Maven, this is done by
placing the appropriate folder structure under `src/main/resources` in your
project root.

Finally, create a config loader in your plugin code:

```java
@Inject PluginContainer plugin;
...
// note that the .conf extension is not required
LBConfig config = LBConfig.provide(plugin, "bar", "quux");
```

And access configuration values like so:

```java
String guava = config.getNode("guava").getString();
int cheese = config.getNode("chocolate", "cheese").getInt();
```

You can also retrive the underlying `configurate` node if you perfer to work
with it directly:

```java
ConfigurationNode node = config.getRootNode();
String guava = node.getNode("guava").getString();
int cheese = node.getNode("chocolate", "cheese").getInt();
```

### License

LightBox is made available under the MIT license. It may be used, modified, and
redistributed within the terms of the license.
