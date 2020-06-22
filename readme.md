# PersistentBlockMetadataAPI

This is an API to enable saving metadata on blocks using area effect clouds.
One cloud per plugin (using the API) per chunk (containing blocks with metadata from that plugin) is used.

## Javadocs

[https://thetechdoodle.github.io/persistent-block-metadata-api/](https://thetechdoodle.github.io/persistent-block-metadata-api/)

## Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.TheTechdoodle</groupId>
    <artifactId>persistent-block-metadata-api</artifactId>
    <version>v1.0.0</version>
</dependency>
```

## Use Case

Advantages:
* Saved block data is synced with chunk saving; even in unexpected situations\
  For example, if the server crashes without saving chunks, the metadata will match the state of
  the blocks in the chunk because it only gets saved when the chunk is saved.
  Using an external database could create a mismatch between metadata and block state.
* This solution uses Spigot's built-in apis
* Saved data can be backed up in a world save and can be copied if the region is copied
* Removing or disabling the plugin storing metadata causes the clouds to fade away - no manual cleanup is needed

Drawbacks:
* Using entities instead of direct Java objects is significantly less efficient
* Uninstalling/disabling a plugin using this API then reinstalling/enabling it will remove saved data
