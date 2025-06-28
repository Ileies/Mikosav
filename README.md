# Mikosav - Minecraft Plugin

## Overview

Mikosav is a comprehensive Minecraft plugin that serves as an interface between my Minecraft server and the RizinOS web
service. It provides a wide range of server management features, economy system, and player data synchronization with
the RizinOS platform.

## Features

- **Player Management**
    - Player data synchronization with RizinOS accounts
    - Permission management system
    - Player visibility controls

- **Economy System**
    - Balance checking and transactions
    - Item worth evaluation
    - Buy and sell items for in-game currency

- **Teleportation**
    - Home system (set/teleport to home)
    - Warp system (create/delete/teleport to warps)
    - Back command (return to death location)
    - Cross-world teleportation with inventory management
    - TPA system (request to teleport to other players)

- **Inventory Management**
    - Access to enderchest
    - Inventory synchronization between worlds
    - Chest protection system

- **Admin Tools**
    - Fly mode
    - Heal command
    - Weather and time control
    - Permission management commands
    - Run command as console

- **API Integration**
    - Seamless integration with RizinOS web service
    - Player data synchronization
    - Economy transactions through web API
    - Cross-server functionality

## Requirements

- Paper/Spigot Minecraft server (version 1.16.5 or compatible)
- Java 8 or higher
- Active internet connection for API communication
- RizinOS account for full functionality

## Installation

1. Download the latest release JAR file from the releases page
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. The plugin will generate default configuration files
5. Ensure your server can connect to the RizinOS API (rizinos.com)

## Configuration

The plugin connects to the RizinOS API automatically. Players will need to link their Minecraft accounts with RizinOS
accounts through the website (https://rizinos.com).

## Commands

The plugin provides over 30 commands for various functionalities:

### Economy Commands

- `/balance [player]` - Check your or another player's balance
- `/pay <player> <amount>` - Pay another player
- `/sell [value]` - Sell items for money
- `/worth` - Check the worth of items in your hand
- `/obtain <value>` - Purchase diamonds with money

### Teleportation Commands

- `/back` - Teleport to your last death location
- `/home` - Teleport to your home
- `/sethome` - Set your home location
- `/spawn` - Teleport to server spawn
- `/warp <warp>` - Teleport to a warp point
- `/setwarp <warp>` - Create a new warp point (requires permission)
- `/delwarp <warp>` - Delete a warp point (requires permission)
- `/tpa <player>` - Request to teleport to another player
- `/tpaccept` - Accept a teleport request
- `/wtp [player] <world>` - Teleport to another world (requires permission)

### Admin Commands

- `/day` - Set clear weather and daytime
- `/fly [username]` - Toggle flight mode
- `/flyspeed <1-10>` - Set flight speed
- `/heal [username]` - Heal a player
- `/giveperm <username> <permission>` - Give a permission to a player
- `/removeperm <username> <permission>` - Remove a permission from a player
- `/hideplayer <player> <target>` - Hide a player from another player
- `/showplayer <player> <target>` - Make a hidden player visible again
- `/run <command>` - Run a command as console
- `/ench` - Enchant the item in your hand

### Utility Commands

- `/ec` - Open your enderchest
- `/protect [off]` - Protect a chest
- `/ms <action> [data]` - Access the Mikosav API directly

## Permissions

The plugin uses a comprehensive permission system:

- `mikosav.command.day` - Use the /day command
- `mikosav.command.ec` - Use the /ec command
- `mikosav.command.ench` - Use the /ench command
- `mikosav.command.fly` - Use the /fly command
- `mikosav.command.flyall` - Use /fly on other players
- `mikosav.command.heal` - Use the /heal command
- `mikosav.command.healall` - Use /heal on other players
- `mikosav.command.hide` - Use player visibility commands
- `mikosav.command.ms` - Access the Mikosav API
- `mikosav.command.perms` - Manage permissions
- `mikosav.command.run` - Run commands as console
- `mikosav.command.setwarp` - Create and delete warps
- `mikosav.command.speed` - Change speed
- `mikosav.command.speedall` - Change others' speed
- `mikosav.command.wtp` - Teleport between worlds
- `mikosav.command.wtpall` - Teleport others between worlds
- `mikosav.bypass.protect` - Bypass chest protection
- `mikosav.bypass.worldchange` - Bypass world change restrictions
- `mikosav.perm.warpsign` - Create warp signs
- `mikosav.admin` - Admin access
- `mikosav.moderator` - Moderator access
- `mikosav.verified` - Verified user access

## API Integration

The plugin connects to the RizinOS API at rizinos.com. This integration provides:

- Player data synchronization
- Economy system
- Permission management
- Cross-world inventory management
- Server configuration synchronization

## Support

For issues, feature requests, or questions, please contact the author or visit the RizinOS website
at https://rizinos.com.

## License

This project is proprietary software. All rights reserved.

## Author

Developed by Ileies
