<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:90c93a6e78ae0ad88264e4e187a035d475451d5609d1219396e156be39847fbf -->

# Christmas

A Minecraft Fabric utility client.

## Build and install

Use Java 25 and run `./gradlew build`. Copy the resulting JAR from `build/libs/` into your Minecraft Fabric mods folder.

Iris remains optional. Multiplayer uses Minecraft's normal connections.

## Rename compatibility

The mod ID and configuration directory are now `christmas-client`, the Java package is `christmas.client`, and addon entrypoints and Starscript bindings use `christmas`. Previous addons must be rebuilt. Existing configuration can be copied into the new directory, but scripts and resource identifiers need updating.

## Source and license

Christmas is derived from [Meteor Client](https://github.com/MeteorDevelopment/meteor-client), copyright Meteor Development, and distributed under GPL-3.0. Original copyright notices and the [license](LICENSE) are retained. This fork modifies rendering, storage discovery, shader compatibility and branding, and removes Discord presence, custom proxies and upstream online/cape services.


<!-- /coderabbit-ai-generated:v1 -->