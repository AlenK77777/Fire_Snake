# Fire Snake Game

A classic Snake game with shooting mechanics implemented in Java.

## Java Version (Maven Project)

### Requirements
- Java JDK 21 or higher
- Maven 3.6+

### Project Structure
```
src/main/java/com/firesnake/
└── FireSnakeGame.java
```

### How to Build and Run with Maven

```bash
# Build the project
mvn clean package

# Run the game
mvn exec:java

# Or run the JAR directly
java -jar target/fire-snake-game-1.0.0.jar
```

### Build in Jenkins
Use the following Maven goals:
```
clean package
```

### Controls
- **Arrow Keys** - Move the snake (Up, Down, Left, Right)
- **Space** - Shoot bullets in the direction of movement
- **Q** - Quit game (after game over)
- **C** - Play again (after game over)

## Game Rules
- Control the snake to eat food (pink circles)
- Each food eaten increases your score and snake length
- **Shoot yellow targets** to earn points and grow your snake
- **WARNING:** Don't shoot the pink food - it ends the game!
- Targets appear randomly (0-5 at a time) and last 5-10 seconds
- Avoid hitting the walls or yourself
- The game ends when you hit a wall, yourself, or shoot the food
