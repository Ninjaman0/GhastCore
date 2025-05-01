# GhastCore

GhastCore is a modular plugin framework for Paper servers, providing player data management, database integration (SQLite/MySQL), and dynamic extension loading.

## Features
- **Player Data Management**: Store and retrieve player data with caching.
- **Database Support**: SQLite or MySQL with HikariCP connection pooling.
- **Extension System**: Load and manage extensions dynamically.
- **API**: Public API for other plugins to interact with GhastCore.

## Installation
1. Download the latest `GhastCore.jar` from [Releases](https://github.com/Ninjaman0/GhastCore/releases).
2. Place the JAR in your server's `plugins` folder.
3. Restart the server.

## API Usage
Add GhastCore as a dependency in your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>com.github.yourusername</groupId>
        <artifactId>GhastCore</artifactId>
        <version>v2.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>