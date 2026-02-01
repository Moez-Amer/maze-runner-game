# 🧙 WIZARD HUNT

A 2D maze-runner game developed using **LibGDX** for the Fundamentals of Programming (FOP) course at TUM.

Navigate through dangerous dungeons, battle enemies, collect keys, avoid deadly traps, and escape through the exit before it's too late!

---

## 📁 Project Structure

```
fopws2526projectfop-oopsitcompiles/
├── assets/                          # Game assets (textures, audio, maps)
│   ├── maps/                        # Map properties files
│   │   ├── level-1.properties       # Level 1 configuration
│   │   ├── level-2.properties       # Level 2 configuration
│   │   ├── level-3.properties       # Level 3 configuration
│   │   ├── level-4.properties       # Level 4 configuration
│   │   ├── level-5.properties       # Level 5 configuration
│   │   └── survival.properties      # Survival mode arena
│   ├── MoriaMap/                    # Tiled TMX source files
│   ├── audio/                       # Music and sound effects
│   ├── Character1_Assets/           # Player sprite sheets
│   ├── Enemy_Assets/                # Enemy animations
│   └── Wizard/                      # Final Boss assets
├── core/                            # Main game source code
│   └── src/de/tum/cit/fop/maze/
│       ├── Screens/                 # All game screens (Menu, Game, etc.)
│       └── *.java                   # Core game classes
├── desktop/                         # Desktop launcher
├── gradle/                          # Gradle wrapper
├── build.gradle                     # Root build configuration
└── README.md                        # This file
```

### Map Folder Location

The `maps/` folder containing all level `.properties` files is located inside the `assets/` folder:

```
assets/maps/
```

---

## 🏗️ Class Hierarchy (OOP Design)

The game follows a clean object-oriented inheritance structure:

### Core Inheritance Tree

```
GameObject (abstract)
├── MovableGameObject (abstract)
│   ├── Player
│   └── Enemy
│       └── FinalBoss
├── Trap (abstract)
│   ├── KnifesTrap
│   └── DeathPitTrap
├── Wall
├── Entry
├── Exit
├── Collectibles
└── VoodooDoll
```

### Class Descriptions

| Class | Description |
|-------|-------------|
| `GameObject` | Abstract base class with position, size, bounds, and `render()` method |
| `MovableGameObject` | Extends GameObject; adds movement, collision detection, animation |
| `Player` | The controllable character with combat, inventory, and skill integration |
| `Enemy` | AI-controlled enemies with A* pathfinding |
| `FinalBoss` | Special boss enemy with aura damage mechanic that appears in Level 5 map |
| `Trap` | Abstract trap superclass with shared collision detection |
| `KnifesTrap` | Animated knife trap that damages on contact |
| `DeathPitTrap` | Instant-death pit trap |
| `Wall` | Impassable wall tile (rendering handled by map renderer) |
| `Entry` | Player spawn point marker |
| `Exit` | Level completion goal |
| `Collectibles` | Pickupable items (keys, potions, scrolls) |
| `VoodooDoll` | Revival mechanic collectible |

---

## 📊 UML Class Diagram

The UML class diagram image is located in:

```
assets/UML/
```

---

## 🎮 How to Run

**Windows:**
```bash
gradlew.bat desktop:run
```

**Linux/macOS:**
```bash
./gradlew desktop:run
```

---

## 🗺️ Map Loading System

The game supports **two distinct map loading modes**, controlled by the first two lines of each `.properties` file:

### 1. Tiled Hybrid Mode (`loader=tiled`)

Uses professional Tiled TMX files for visual rendering while using the properties file for game logic.

```properties
loader=tiled
tmx_file=MoriaMap/newmap1.tmx
```

**Features:**
- Beautiful hand-crafted visuals from Tiled editor
- TMX file handles all graphical rendering
- Properties file provides collision data and entity positions
- Best visual quality

### 2. Properties-Only Mode (`loader=custom`)

Loads everything purely from the properties file without any TMX dependency.

**Features:**
- No external TMX file required
- Game generates its own tile rendering
- Useful for programmatically generated maps
- Complies with the pure-properties requirement

### Tile Type Constants

| Value | Type | Description |
|-------|------|-------------|
| 0 | `TYPE_WALL` | Impassable wall |
| 1 | `TYPE_ENTRY` | Player spawn point |
| 2 | `TYPE_EXIT` | Level exit |
| 3 | `TYPE_KNIFE_TRAP` | Knife trap (damage) |
| 4 | `TYPE_ENEMY` | Enemy spawn point |
| 5 | `TYPE_KEY` | Collectible key |
| 6 | `TYPE_DEATHTRAP` | Instant death pit |
| 7 | `TYPE_PATH` | Walkable floor |
| 8 | `TYPE_BOSS` | Final boss spawn |

### TiledToPropertiesConverter

A custom converter tool (`TiledToPropertiesConverter.java`) is provided to convert Tiled TMX maps into properties files:

```bash
java TiledToPropertiesConverter <input.tmx> <output.properties>
```

---

## 🎯 Game Controls

| Key     | Action              |
|---------|---------------------|
| `↑`     | Move Up             |
| `←`     | Move Left           |
| `↓`     | Move Down           |
| `→`     | Move Right          |
| `SPACE` | Attack              |
| `ESC`   | Pause Game          |
| `TAB`   | Developer Console   |
| `U`     | Debug Visualization |


Controls are fully remappable in the Settings menu.

---

## ✨ Features Beyond Minimal Requirements

### 🗡️ Combat System
- **Combo Attacks**: Chain attacks for increased damage
- **Directional Combat**: Attack animations for all 4 directions
- **Kill Streaks**: Visual feedback for consecutive kills
  - Double Kill, Triple Kill, Mega Kill, etc.

### 👻 Revival mode using Voodoo doll (Ghost Mode)
- When the player dies, they become a ghost
- A **Voodoo Doll** appears at the death location
- Player has limited time to reach the doll and revive
- Adds strategic depth to dangerous areas

### 🧙 Final Boss Battle
- **The Wizard**: A powerful boss on Level 5
- **Aura of Despair**: Damaging aura that hurts nearby players
- **Multi-phase Combat**: Requires strategy to defeat
- Unique animations and death sequence

### 📜 Scroll Collectibles (Additional Exit Requirement)
- **Magic Scrolls**: Ancient scrolls containing passage spells required alongside keys to unlock the exit
- Adds an extra layer of exploration and collection to complete each level
- Thematically enhances the wizard-hunting narrative

### 🎖️ Achievement System with Emblems
- **15+ Achievements with unique Emblems**: Track player progress that have Emblems that appear in gameplay
- **Observer Pattern**: Real-time unlock notifications
- Categories: Combat, Exploration, Survival, Collection

### 🎬 Video Cutscenes (Trailer)
- Intro cutscene before main menu
- Story-driven narrative elements
- Skippable for returning players

### 🔍 Extensive Developer Console
Press `TAB` in-game to access powerful debug commands:
- `help` - List all commands
- `give_item <item>` - Add items to inventory
- `god` - Toggle invincibility
- `noclip` - Toggle collision
- `kill_all` - Kill all enemies
- `spawn_enemy <x> <y>` - Spawn enemy at position
- `teleport <x> <y>` - Move player to coordinates
- `complete_level` - Instantly win
- And more...

### 🔄 Dual Map Loading System
- **Two distinct loading methods**: Tiled Hybrid Mode and Properties-Only Mode
- Used **Tiled editor** to create beautiful, hand-crafted unique maps with professional visuals
- Properties-only mode available for pure programmatic map generation

### 🛠️ TiledToPropertiesConverter Tool
- Custom-built converter to transform Tiled TMX maps into game-compatible properties files
- Automates the conversion process for rapid level development
- Supports all tile types including walls, traps, enemies, and boss spawns

### 🗺️ Created 5 Unique Maps using Tiled
- Choose any unlocked level from the menu
- Visual progression indicator
- Quick restart functionality

### 💾 Extensive Multi-Profile Save System
- **Multiple Player Profiles**: Each player has their own save
  - Completed levels
  - Unlocked achievements
  - Skill points and upgrades
  - Survival high scores
  - Statistics (kills, deaths, time played)

### 👁️ Debug Visualization Mode
- **Real-time Hitbox Overlay**: Press U to instantly toggle visible collision boxes for players, enemies, and walls.
- **Object Tracking**: Visualize the interaction boundaries of every world object to ensure perfect mechanical precision.
- **Developer Insight**: A custom-built tool designed to assist in testing physics and refining the gameplay experience across all five maps.
---


## 👥 Team: oops;itcompiles

FOP WS25/26 - Technical University of Munich (TUM)