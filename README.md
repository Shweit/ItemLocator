# ItemLocator
<img src="https://img.shields.io/github/actions/workflow/status/Shweit/ItemLocator/runtime.yml" /> <img src="https://img.shields.io/github/v/release/Shweit/ItemLocator" /> <img src="https://img.shields.io/github/license/Shweit/ItemLocator" />

## Overview
ItemLocator helps players track and locate items in Minecraft chests by marking their locations with glowing beacons, making organization and retrieval easier.

## Features
- **Track Item Locations:** Automatically track the locations of items placed in chests.
- **Glowing Beacons:** Visualize item locations using glowing beacons for easy identification.
- **Player-Specific Views:** Each player has their own view of chest contents, ensuring privacy.

## Commands
`/itemlocator gui` - Opens the GUI displaying all tracked items.
- **Description:** Opens a GUI where players can view all items they have previously stored and their locations.

`/itemlocator locate <item_name>` - Searches for the specified item and displays its locations.
- **Description:** Finds the locations of a specific item stored in chests.

## Installation
### Prerequisites
- **Java:** JDK 20 or higher is required to build and run the project.
- **Gradle:** Make sure Gradle is installed on your system.

### Cloning the Repository
1. Clone the repository to your local machine.
```shell
git clone git@github.com:Shweit/ItemLocator.git
cd ItemLocator
```
### Building the Project
2. Build the project using Gradle.
```shell
gradle build
```
### Setting up the Minecraft Server
3. Copy the generated JAR file to the `plugins` directory of your Minecraft server.
```shell
cp build/libs/ItemLocator-*.jar /path/to/your/minecraft/server/plugins
```
4. Start or restart your Minecraft server.
```shell
java -Xmx1024M -Xms1024M -jar paper-1.21.jar nogui
```
5.  Once the server is running, the plugin will be loaded automatically. You can verify it by running:
```shell
/plugins
```

## Contributing
Contributions are welcome! Please read the [contributing guidelines](CONTRIBUTING.md) to get started.