package com.firesnake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class FireSnakeGame extends JPanel implements ActionListener, KeyListener {
    
    // Screen dimensions
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    private static final int STATS_WIDTH = 220;
    private static final int TOTAL_WIDTH = GAME_WIDTH + STATS_WIDTH;
    
    // Snake properties
    private static final int BLOCK_SIZE = 20;
    private static final int NORMAL_DELAY = 1000 / 12; // 12 FPS
    private static final int SLOW_DELAY = 1000 / 6;    // 6 FPS (2x slower)
    private int currentDelay = NORMAL_DELAY;
    
    // Modern color palette
    private static final Color BACKGROUND_COLOR_1 = new Color(5, 5, 15);
    private static final Color BACKGROUND_COLOR_2 = new Color(15, 15, 35);
    private static final Color GRID_COLOR = new Color(40, 40, 80, 30);
    private static final Color STATS_BG = new Color(10, 10, 25);
    
    // Snake gradient colors
    private static final Color SNAKE_HEAD_COLOR = new Color(0, 255, 150);
    private static final Color SNAKE_BODY_START = new Color(0, 200, 100);
    private static final Color SNAKE_BODY_END = new Color(0, 100, 50);
    private static final Color SNAKE_GLOW = new Color(0, 255, 150, 80);
    private static final Color SNAKE_SLOW_COLOR = new Color(100, 150, 255); // Blue tint when slowed
    
    // Food colors
    private static final Color FOOD_COLOR = new Color(255, 50, 100);
    private static final Color FOOD_GLOW = new Color(255, 50, 100, 100);
    private static final Color FOOD_INNER = new Color(255, 150, 180);
    
    // UI colors
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final Color SCORE_GLOW = new Color(0, 255, 150, 100);
    
    // Particle colors
    private static final Color[] PARTICLE_COLORS = {
        new Color(255, 100, 150),
        new Color(255, 200, 100),
        new Color(100, 255, 200),
        new Color(200, 100, 255)
    };
    
    // Bullet colors
    private static final Color BULLET_COLOR = new Color(0, 255, 255);
    private static final Color BULLET_GLOW = new Color(0, 255, 255, 100);
    
    // Slowdown target color
    private static final Color SLOW_TARGET_COLOR = new Color(100, 150, 255);
    private static final Color SLOW_TARGET_INNER = new Color(180, 200, 255);
    
    // Shrink target color (red/dark)
    private static final Color SHRINK_TARGET_COLOR = new Color(255, 50, 50);
    private static final Color SHRINK_TARGET_INNER = new Color(255, 150, 150);
    
    // Target types enum (SQUARE dangerous targets)
    private enum TargetType {
        COMMON(new Color(255, 200, 50), new Color(255, 255, 150), 1, "Common"),
        FAST(new Color(50, 200, 255), new Color(150, 230, 255), 2, "Fast"),
        RARE(new Color(200, 50, 255), new Color(230, 150, 255), 3, "Rare"),
        EPIC(new Color(255, 100, 50), new Color(255, 180, 100), 5, "Epic"),
        LEGENDARY(new Color(255, 215, 0), new Color(255, 245, 150), 10, "Legendary");
        
        final Color color;
        final Color innerColor;
        final int points;
        final String name;
        
        TargetType(Color color, Color innerColor, int points, String name) {
            this.color = color;
            this.innerColor = innerColor;
            this.points = points;
            this.name = name;
        }
    }
    
    // Game state
    private ArrayList<int[]> snakeList;
    private int snakeLength;
    private int x1, y1;
    private int x1Change, y1Change;
    private int foodX, foodY;
    private int score;
    private int highScore;
    private boolean gameOver;
    private boolean gameClose;
    private boolean gameStarted;
    
    // Slowdown effect
    private int slowdownTimer = 0;
    private static final int SLOWDOWN_DURATION = 120; // 10 seconds at 12 FPS
    
    // Statistics
    private int totalShots;
    private int targetsHit;
    private int foodEaten;
    
    // Direction queue for smooth controls
    private Queue<int[]> directionQueue;
    private static final int MAX_QUEUE_SIZE = 3;
    
    // Animation variables
    private float foodPulse = 0;
    private float backgroundOffset = 0;
    private ArrayList<Particle> particles;
    
    // Hyperspace stars
    private ArrayList<Star> stars;
    private static final int NUM_STARS = 100;
    
    // Shooting and targets
    private ArrayList<Bullet> bullets;
    private ArrayList<Target> targets;
    private ArrayList<SlowTarget> slowTargets;
    private ArrayList<ShrinkTarget> shrinkTargets;
    private int targetSpawnTimer = 0;
    private static final int TARGET_SPAWN_INTERVAL = 60;
    private static final int BULLET_SPEED = 50;
    
    private Timer timer;
    private Random random;
    
    // Star class for hyperspace effect - stars fly towards us from center
    private class Star {
        float x, y;       // Position relative to center (-1 to 1)
        float z;          // Depth (distance from viewer)
        float speed;
        
        Star() {
            // Random position around center
            double angle = random.nextDouble() * Math.PI * 2;
            float dist = 0.1f + random.nextFloat() * 0.9f;
            x = (float) Math.cos(angle) * dist;
            y = (float) Math.sin(angle) * dist;
            z = random.nextFloat(); // Random initial depth
            speed = 0.003f + random.nextFloat() * 0.007f;
        }
        
        void reset() {
            // Reset to far away, random angle
            double angle = random.nextDouble() * Math.PI * 2;
            float dist = 0.1f + random.nextFloat() * 0.5f;
            x = (float) Math.cos(angle) * dist;
            y = (float) Math.sin(angle) * dist;
            z = 1.0f;
            speed = 0.003f + random.nextFloat() * 0.007f;
        }
        
        void update() {
            z -= speed;
            if (z <= 0.01f) {
                reset();
            }
        }
        
        void draw(Graphics2D g2d) {
            // Project to screen - stars fly outward from center
            float screenX = GAME_WIDTH / 2f + (x / z) * GAME_WIDTH * 0.5f;
            float screenY = GAME_HEIGHT / 2f + (y / z) * GAME_HEIGHT * 0.5f;
            
            // Previous position for trail
            float prevZ = z + speed * 2;
            float prevScreenX = GAME_WIDTH / 2f + (x / prevZ) * GAME_WIDTH * 0.5f;
            float prevScreenY = GAME_HEIGHT / 2f + (y / prevZ) * GAME_HEIGHT * 0.5f;
            
            // Check bounds
            if (screenX < 0 || screenX > GAME_WIDTH || screenY < 0 || screenY > GAME_HEIGHT) {
                reset();
                return;
            }
            
            // Brightness and size based on proximity (closer = brighter/larger)
            float proximity = 1.0f - z;
            int brightness = (int) Math.min(255, proximity * 300);
            float size = 1 + proximity * 2;
            
            if (brightness > 20) {
                // Draw trail line
                g2d.setColor(new Color(200, 200, 255, brightness / 3));
                g2d.setStroke(new BasicStroke(Math.max(0.5f, size * 0.3f)));
                g2d.drawLine((int) prevScreenX, (int) prevScreenY, (int) screenX, (int) screenY);
                
                // Draw star as small circle (dot)
                g2d.setColor(new Color(255, 255, 255, brightness));
                int dotSize = Math.max(1, (int) size);
                g2d.fillOval((int)(screenX - dotSize/2), (int)(screenY - dotSize/2), dotSize, dotSize);
            }
        }
    }
    
    // Particle class for effects
    private class Particle {
        float x, y;
        float vx, vy;
        float life;
        float maxLife;
        Color color;
        float size;
        
        Particle(float x, float y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = (random.nextFloat() - 0.5f) * 10;
            this.vy = (random.nextFloat() - 0.5f) * 10;
            this.maxLife = 25 + random.nextInt(15);
            this.life = maxLife;
            this.size = 3 + random.nextFloat() * 5;
        }
        
        void update() {
            x += vx;
            y += vy;
            vx *= 0.95f;
            vy *= 0.95f;
            life--;
        }
        
        boolean isDead() {
            return life <= 0;
        }
        
        void draw(Graphics2D g2d) {
            float alpha = life / maxLife;
            Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
            g2d.setColor(c);
            float currentSize = size * alpha;
            g2d.fill(new Ellipse2D.Float(x - currentSize/2, y - currentSize/2, currentSize, currentSize));
        }
    }
    
    // Bullet class for shooting
    private class Bullet {
        float x, y;
        float vx, vy;
        
        Bullet(float x, float y, int dirX, int dirY) {
            this.x = x;
            this.y = y;
            if (dirX != 0 || dirY != 0) {
                float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
                this.vx = (dirX / length) * BULLET_SPEED;
                this.vy = (dirY / length) * BULLET_SPEED;
            } else {
                this.vx = 0;
                this.vy = -BULLET_SPEED;
            }
        }
        
        void update() {
            x += vx;
            y += vy;
        }
        
        boolean isOutOfBounds() {
            return x < 0 || x > GAME_WIDTH || y < 0 || y > GAME_HEIGHT;
        }
        
        void draw(Graphics2D g2d) {
            g2d.setColor(BULLET_GLOW);
            g2d.fill(new Ellipse2D.Float(x - 8, y - 8, 16, 16));
            g2d.setColor(BULLET_COLOR);
            g2d.fill(new Ellipse2D.Float(x - 5, y - 5, 10, 10));
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fill(new Ellipse2D.Float(x - 3, y - 3, 4, 4));
        }
    }
    
    // Target class - SQUARE shape (dangerous, can't pass through)
    private class Target {
        int x, y;
        float lifetime;
        float maxLifetime;
        float pulse = 0;
        TargetType type;
        
        Target(int x, int y, TargetType type) {
            this.x = x;
            this.y = y;
            this.type = type;
            float baseLife = 60 + random.nextInt(61);
            this.maxLifetime = baseLife / (1 + type.ordinal() * 0.2f);
            this.lifetime = maxLifetime;
        }
        
        void update() {
            lifetime--;
            pulse += 0.2f + type.ordinal() * 0.05f;
        }
        
        boolean isDead() {
            return lifetime <= 0;
        }
        
        void draw(Graphics2D g2d) {
            float alpha = Math.min(1.0f, lifetime / 30.0f);
            float pulseScale = (float)(Math.sin(pulse) * 0.1 + 1);
            int size = (int)(BLOCK_SIZE * pulseScale);
            int offset = (BLOCK_SIZE - size) / 2;
            
            // Draw outer glow (square)
            for (int i = 3; i > 0; i--) {
                int glowSize = size + i * 6;
                int glowOffset = (BLOCK_SIZE - glowSize) / 2;
                g2d.setColor(new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), (int)((30 - i * 8) * alpha)));
                g2d.fill(new RoundRectangle2D.Float(x + glowOffset, y + glowOffset, glowSize, glowSize, 4, 4));
            }
            
            // Draw target SQUARE
            GradientPaint targetGradient = new GradientPaint(
                x, y, new Color(type.innerColor.getRed(), type.innerColor.getGreen(), type.innerColor.getBlue(), (int)(255 * alpha)),
                x + BLOCK_SIZE, y + BLOCK_SIZE, new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), (int)(255 * alpha))
            );
            g2d.setPaint(targetGradient);
            g2d.fill(new RoundRectangle2D.Float(x + offset, y + offset, size, size, 4, 4));
            
            // Draw X pattern (danger indicator)
            g2d.setColor(new Color(255, 255, 255, (int)(200 * alpha)));
            g2d.setStroke(new BasicStroke(2));
            int centerX = x + BLOCK_SIZE / 2;
            int centerY = y + BLOCK_SIZE / 2;
            int crossSize = size / 4;
            g2d.drawLine(centerX - crossSize, centerY - crossSize, centerX + crossSize, centerY + crossSize);
            g2d.drawLine(centerX + crossSize, centerY - crossSize, centerX - crossSize, centerY + crossSize);
            
            // Draw border
            g2d.setColor(new Color(255, 255, 255, (int)(100 * alpha)));
            g2d.draw(new RoundRectangle2D.Float(x + offset, y + offset, size, size, 4, 4));
            
            // Draw points indicator
            g2d.setColor(new Color(255, 255, 255, (int)(180 * alpha)));
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String pts = "+" + type.points;
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(pts, centerX - fm.stringWidth(pts)/2, y - 2);
            
            // Draw lifetime bar
            float lifePercent = lifetime / maxLifetime;
            int barWidth = (int)(BLOCK_SIZE * lifePercent);
            g2d.setColor(new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), (int)(150 * alpha)));
            g2d.fillRect(x, y + BLOCK_SIZE + 2, barWidth, 3);
        }
    }
    
    // Slow Target class - CIRCLE shape (safe to pass, slows snake when shot)
    private class SlowTarget {
        int x, y;
        float lifetime;
        float maxLifetime;
        float pulse = 0;
        
        SlowTarget(int x, int y) {
            this.x = x;
            this.y = y;
            this.maxLifetime = 80 + random.nextInt(40);
            this.lifetime = maxLifetime;
        }
        
        void update() {
            lifetime--;
            pulse += 0.15f;
        }
        
        boolean isDead() {
            return lifetime <= 0;
        }
        
        void draw(Graphics2D g2d) {
            float alpha = Math.min(1.0f, lifetime / 30.0f);
            float pulseScale = (float)(Math.sin(pulse) * 0.15 + 1);
            int size = (int)(BLOCK_SIZE * pulseScale);
            int offset = (BLOCK_SIZE - size) / 2;
            
            // Draw outer glow (circle)
            for (int i = 3; i > 0; i--) {
                int glowSize = size + i * 6;
                int glowOffset = (BLOCK_SIZE - glowSize) / 2;
                g2d.setColor(new Color(SLOW_TARGET_COLOR.getRed(), SLOW_TARGET_COLOR.getGreen(), SLOW_TARGET_COLOR.getBlue(), (int)((30 - i * 8) * alpha)));
                g2d.fill(new Ellipse2D.Float(x + glowOffset, y + glowOffset, glowSize, glowSize));
            }
            
            // Draw target CIRCLE
            GradientPaint targetGradient = new GradientPaint(
                x, y, new Color(SLOW_TARGET_INNER.getRed(), SLOW_TARGET_INNER.getGreen(), SLOW_TARGET_INNER.getBlue(), (int)(255 * alpha)),
                x + BLOCK_SIZE, y + BLOCK_SIZE, new Color(SLOW_TARGET_COLOR.getRed(), SLOW_TARGET_COLOR.getGreen(), SLOW_TARGET_COLOR.getBlue(), (int)(255 * alpha))
            );
            g2d.setPaint(targetGradient);
            g2d.fill(new Ellipse2D.Float(x + offset, y + offset, size, size));
            
            // Draw slow icon (hourglass-like)
            g2d.setColor(new Color(255, 255, 255, (int)(200 * alpha)));
            g2d.setStroke(new BasicStroke(2));
            int centerX = x + BLOCK_SIZE / 2;
            int centerY = y + BLOCK_SIZE / 2;
            g2d.drawLine(centerX - 4, centerY - 4, centerX + 4, centerY - 4);
            g2d.drawLine(centerX - 4, centerY + 4, centerX + 4, centerY + 4);
            g2d.drawLine(centerX - 4, centerY - 4, centerX, centerY);
            g2d.drawLine(centerX + 4, centerY - 4, centerX, centerY);
            g2d.drawLine(centerX - 4, centerY + 4, centerX, centerY);
            g2d.drawLine(centerX + 4, centerY + 4, centerX, centerY);
            
            // Draw "SLOW" text
            g2d.setColor(new Color(255, 255, 255, (int)(150 * alpha)));
            g2d.setFont(new Font("Arial", Font.BOLD, 8));
            g2d.drawString("SLOW", x - 2, y - 2);
            
            // Draw lifetime bar
            float lifePercent = lifetime / maxLifetime;
            int barWidth = (int)(BLOCK_SIZE * lifePercent);
            g2d.setColor(new Color(SLOW_TARGET_COLOR.getRed(), SLOW_TARGET_COLOR.getGreen(), SLOW_TARGET_COLOR.getBlue(), (int)(150 * alpha)));
            g2d.fillRect(x, y + BLOCK_SIZE + 2, barWidth, 3);
        }
    }
    
    // Shrink Target class - TRIANGLE shape (safe to pass, shrinks snake by half when shot)
    private class ShrinkTarget {
        int x, y;
        float lifetime;
        float maxLifetime;
        float pulse = 0;
        
        ShrinkTarget(int x, int y) {
            this.x = x;
            this.y = y;
            this.maxLifetime = 70 + random.nextInt(50);
            this.lifetime = maxLifetime;
        }
        
        void update() {
            lifetime--;
            pulse += 0.18f;
        }
        
        boolean isDead() {
            return lifetime <= 0;
        }
        
        void draw(Graphics2D g2d) {
            float alpha = Math.min(1.0f, lifetime / 30.0f);
            float pulseScale = (float)(Math.sin(pulse) * 0.15 + 1);
            int size = (int)(BLOCK_SIZE * pulseScale);
            int offset = (BLOCK_SIZE - size) / 2;
            
            // Draw outer glow (triangle shape)
            int centerX = x + BLOCK_SIZE / 2;
            int centerY = y + BLOCK_SIZE / 2;
            
            for (int i = 3; i > 0; i--) {
                int glowSize = size + i * 4;
                int[] xPoints = {centerX, centerX - glowSize/2, centerX + glowSize/2};
                int[] yPoints = {centerY - glowSize/2, centerY + glowSize/2, centerY + glowSize/2};
                g2d.setColor(new Color(SHRINK_TARGET_COLOR.getRed(), SHRINK_TARGET_COLOR.getGreen(), SHRINK_TARGET_COLOR.getBlue(), (int)((25 - i * 6) * alpha)));
                g2d.fillPolygon(xPoints, yPoints, 3);
            }
            
            // Draw target TRIANGLE
            int halfSize = size / 2;
            int[] xPoints = {centerX, centerX - halfSize, centerX + halfSize};
            int[] yPoints = {centerY - halfSize, centerY + halfSize, centerY + halfSize};
            
            GradientPaint targetGradient = new GradientPaint(
                centerX, centerY - halfSize, new Color(SHRINK_TARGET_INNER.getRed(), SHRINK_TARGET_INNER.getGreen(), SHRINK_TARGET_INNER.getBlue(), (int)(255 * alpha)),
                centerX, centerY + halfSize, new Color(SHRINK_TARGET_COLOR.getRed(), SHRINK_TARGET_COLOR.getGreen(), SHRINK_TARGET_COLOR.getBlue(), (int)(255 * alpha))
            );
            g2d.setPaint(targetGradient);
            g2d.fillPolygon(xPoints, yPoints, 3);
            
            // Draw down arrow inside (shrink indicator)
            g2d.setColor(new Color(255, 255, 255, (int)(200 * alpha)));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(centerX, centerY - 3, centerX, centerY + 4);
            g2d.drawLine(centerX - 3, centerY + 1, centerX, centerY + 4);
            g2d.drawLine(centerX + 3, centerY + 1, centerX, centerY + 4);
            
            // Draw "/2" text
            g2d.setColor(new Color(255, 255, 255, (int)(150 * alpha)));
            g2d.setFont(new Font("Arial", Font.BOLD, 8));
            g2d.drawString("/2", x + 5, y - 2);
            
            // Draw lifetime bar
            float lifePercent = lifetime / maxLifetime;
            int barWidth = (int)(BLOCK_SIZE * lifePercent);
            g2d.setColor(new Color(SHRINK_TARGET_COLOR.getRed(), SHRINK_TARGET_COLOR.getGreen(), SHRINK_TARGET_COLOR.getBlue(), (int)(150 * alpha)));
            g2d.fillRect(x, y + BLOCK_SIZE + 2, barWidth, 3);
        }
    }
    
    public FireSnakeGame() {
        setPreferredSize(new Dimension(TOTAL_WIDTH, GAME_HEIGHT));
        setBackground(BACKGROUND_COLOR_1);
        setFocusable(true);
        addKeyListener(this);
        
        random = new Random();
        particles = new ArrayList<>();
        bullets = new ArrayList<>();
        targets = new ArrayList<>();
        slowTargets = new ArrayList<>();
        shrinkTargets = new ArrayList<>();
        directionQueue = new LinkedList<>();
        
        // Initialize stars for hyperspace effect
        stars = new ArrayList<>();
        for (int i = 0; i < NUM_STARS; i++) {
            stars.add(new Star());
        }
        
        highScore = 0;
        initGame();
        
        timer = new Timer(currentDelay, this);
        timer.start();
    }
    
    private void initGame() {
        snakeList = new ArrayList<>();
        snakeLength = 1;
        directionQueue.clear();
        
        x1 = (GAME_WIDTH / 2 / BLOCK_SIZE) * BLOCK_SIZE;
        y1 = (GAME_HEIGHT / 2 / BLOCK_SIZE) * BLOCK_SIZE;
        x1Change = 0;
        y1Change = 0;
        
        spawnFood();
        
        score = 0;
        totalShots = 0;
        targetsHit = 0;
        foodEaten = 0;
        slowdownTimer = 0;
        currentDelay = NORMAL_DELAY;
        if (timer != null) {
            timer.setDelay(currentDelay);
        }
        
        gameOver = false;
        gameClose = false;
        gameStarted = false;
        particles.clear();
        bullets.clear();
        targets.clear();
        slowTargets.clear();
        shrinkTargets.clear();
        targetSpawnTimer = 0;
    }
    
    private void spawnFood() {
        boolean validPosition;
        do {
            validPosition = true;
            foodX = (random.nextInt((GAME_WIDTH - BLOCK_SIZE * 2) / BLOCK_SIZE) + 1) * BLOCK_SIZE;
            foodY = (random.nextInt((GAME_HEIGHT - BLOCK_SIZE * 2) / BLOCK_SIZE) + 1) * BLOCK_SIZE;
            
            for (int[] segment : snakeList) {
                if (segment[0] == foodX && segment[1] == foodY) {
                    validPosition = false;
                    break;
                }
            }
        } while (!validPosition);
    }
    
    private void spawnParticles(int x, int y, int count, Color baseColor) {
        for (int i = 0; i < count; i++) {
            Color color = baseColor != null ? baseColor : PARTICLE_COLORS[random.nextInt(PARTICLE_COLORS.length)];
            particles.add(new Particle(x + BLOCK_SIZE/2, y + BLOCK_SIZE/2, color));
        }
    }
    
    private TargetType getRandomTargetType() {
        int roll = random.nextInt(100);
        if (roll < 50) return TargetType.COMMON;
        if (roll < 75) return TargetType.FAST;
        if (roll < 90) return TargetType.RARE;
        if (roll < 98) return TargetType.EPIC;
        return TargetType.LEGENDARY;
    }
    
    private void spawnTargets() {
        int count = random.nextInt(6);
        
        for (int i = 0; i < count; i++) {
            int attempts = 0;
            boolean validPosition = false;
            int tx = 0, ty = 0;
            
            while (!validPosition && attempts < 50) {
                tx = (random.nextInt((GAME_WIDTH - BLOCK_SIZE * 2) / BLOCK_SIZE) + 1) * BLOCK_SIZE;
                ty = (random.nextInt((GAME_HEIGHT - BLOCK_SIZE * 2) / BLOCK_SIZE) + 1) * BLOCK_SIZE;
                validPosition = isValidTargetPosition(tx, ty);
                attempts++;
            }
            
            if (validPosition) {
                // 15% chance for slow target, 10% for shrink target, 75% for dangerous target
                int roll = random.nextInt(100);
                if (roll < 15) {
                    slowTargets.add(new SlowTarget(tx, ty));
                } else if (roll < 25) {
                    shrinkTargets.add(new ShrinkTarget(tx, ty));
                } else {
                    targets.add(new Target(tx, ty, getRandomTargetType()));
                }
            }
        }
    }
    
    private boolean isValidTargetPosition(int tx, int ty) {
        if (tx == foodX && ty == foodY) return false;
        
        for (int[] segment : snakeList) {
            if (segment[0] == tx && segment[1] == ty) return false;
        }
        
        for (Target t : targets) {
            if (t.x == tx && t.y == ty) return false;
        }
        
        for (SlowTarget st : slowTargets) {
            if (st.x == tx && st.y == ty) return false;
        }
        
        for (ShrinkTarget sht : shrinkTargets) {
            if (sht.x == tx && sht.y == ty) return false;
        }
        
        return true;
    }
    
    private void shoot() {
        if (!gameStarted || gameClose) return;
        if (snakeList.isEmpty()) return;
        
        int[] head = snakeList.get(snakeList.size() - 1);
        float startX = head[0] + BLOCK_SIZE / 2;
        float startY = head[1] + BLOCK_SIZE / 2;
        
        int dirX = x1Change;
        int dirY = y1Change;
        
        if (dirX == 0 && dirY == 0) {
            dirY = -BLOCK_SIZE;
        }
        
        bullets.add(new Bullet(startX, startY, dirX, dirY));
        totalShots++;
    }
    
    private void activateSlowdown() {
        slowdownTimer = SLOWDOWN_DURATION;
        currentDelay = SLOW_DELAY;
        timer.setDelay(currentDelay);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Draw game area
        drawBackground(g2d);
        drawStars(g2d);
        drawGrid(g2d);
        
        if (gameClose) {
            drawGameOverScreen(g2d);
        } else if (!gameStarted) {
            drawStartScreen(g2d);
            drawFood(g2d);
            drawSnake(g2d);
        } else {
            drawTargets(g2d);
            drawSlowTargets(g2d);
            drawShrinkTargets(g2d);
            drawFood(g2d);
            drawSnake(g2d);
            drawBullets(g2d);
            drawParticles(g2d);
            
            // Draw slowdown indicator
            if (slowdownTimer > 0) {
                drawSlowdownIndicator(g2d);
            }
        }
        
        // Draw stats panel
        drawStatsPanel(g2d);
    }
    
    private void drawBackground(Graphics2D g2d) {
        GradientPaint gradient = new GradientPaint(
            0, 0, BACKGROUND_COLOR_1,
            GAME_WIDTH, GAME_HEIGHT, BACKGROUND_COLOR_2
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
    }
    
    private void drawStars(Graphics2D g2d) {
        for (Star star : stars) {
            star.draw(g2d);
        }
    }
    
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(1));
        
        for (int x = 0; x < GAME_WIDTH; x += BLOCK_SIZE) {
            g2d.drawLine(x, 0, x, GAME_HEIGHT);
        }
        for (int y = 0; y < GAME_HEIGHT; y += BLOCK_SIZE) {
            g2d.drawLine(0, y, GAME_WIDTH, y);
        }
        
        // Border color changes when slowed
        Color borderColor = slowdownTimer > 0 ? SLOW_TARGET_COLOR : new Color(0, 255, 150);
        g2d.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 100));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(2, 2, GAME_WIDTH - 4, GAME_HEIGHT - 4);
        
        g2d.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 200));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(2, 2, GAME_WIDTH - 4, GAME_HEIGHT - 4);
    }
    
    private void drawSnake(Graphics2D g2d) {
        int size = snakeList.size();
        boolean isSlowed = slowdownTimer > 0;
        
        for (int i = 0; i < size; i++) {
            int[] segment = snakeList.get(i);
            float progress = (float) i / Math.max(size - 1, 1);
            
            Color segmentColor;
            if (i == size - 1) {
                segmentColor = isSlowed ? SNAKE_SLOW_COLOR : SNAKE_HEAD_COLOR;
                Color glowColor = isSlowed ? new Color(100, 150, 255, 80) : SNAKE_GLOW;
                g2d.setColor(glowColor);
                g2d.fill(new Ellipse2D.Float(segment[0] - 4, segment[1] - 4, BLOCK_SIZE + 8, BLOCK_SIZE + 8));
            } else {
                if (isSlowed) {
                    int r = (int)(150 + (100 - 150) * (1 - progress));
                    int gr = (int)(180 + (150 - 180) * (1 - progress));
                    int b = (int)(255 + (200 - 255) * (1 - progress));
                    segmentColor = new Color(r, gr, b);
                } else {
                    int r = (int)(SNAKE_BODY_START.getRed() + (SNAKE_BODY_END.getRed() - SNAKE_BODY_START.getRed()) * (1 - progress));
                    int gr = (int)(SNAKE_BODY_START.getGreen() + (SNAKE_BODY_END.getGreen() - SNAKE_BODY_START.getGreen()) * (1 - progress));
                    int b = (int)(SNAKE_BODY_START.getBlue() + (SNAKE_BODY_END.getBlue() - SNAKE_BODY_START.getBlue()) * (1 - progress));
                    segmentColor = new Color(r, gr, b);
                }
            }
            
            g2d.setColor(segmentColor);
            g2d.fill(new RoundRectangle2D.Float(segment[0] + 1, segment[1] + 1, BLOCK_SIZE - 2, BLOCK_SIZE - 2, 6, 6));
            
            GradientPaint shine = new GradientPaint(
                segment[0], segment[1], new Color(255, 255, 255, 80),
                segment[0], segment[1] + BLOCK_SIZE, new Color(255, 255, 255, 0)
            );
            g2d.setPaint(shine);
            g2d.fill(new RoundRectangle2D.Float(segment[0] + 2, segment[1] + 2, BLOCK_SIZE - 4, BLOCK_SIZE / 2 - 2, 4, 4));
            
            if (i == size - 1) {
                drawSnakeEyes(g2d, segment[0], segment[1]);
            }
        }
    }
    
    private void drawSnakeEyes(Graphics2D g2d, int x, int y) {
        int eyeSize = 5;
        int pupilSize = 3;
        int eye1X, eye1Y, eye2X, eye2Y;
        
        if (x1Change > 0) {
            eye1X = x + BLOCK_SIZE - 7; eye1Y = y + 4;
            eye2X = x + BLOCK_SIZE - 7; eye2Y = y + BLOCK_SIZE - 9;
        } else if (x1Change < 0) {
            eye1X = x + 2; eye1Y = y + 4;
            eye2X = x + 2; eye2Y = y + BLOCK_SIZE - 9;
        } else if (y1Change > 0) {
            eye1X = x + 4; eye1Y = y + BLOCK_SIZE - 7;
            eye2X = x + BLOCK_SIZE - 9; eye2Y = y + BLOCK_SIZE - 7;
        } else {
            eye1X = x + 4; eye1Y = y + 2;
            eye2X = x + BLOCK_SIZE - 9; eye2Y = y + 2;
        }
        
        g2d.setColor(Color.WHITE);
        g2d.fillOval(eye1X, eye1Y, eyeSize, eyeSize);
        g2d.fillOval(eye2X, eye2Y, eyeSize, eyeSize);
        g2d.setColor(new Color(20, 20, 40));
        g2d.fillOval(eye1X + 1, eye1Y + 1, pupilSize, pupilSize);
        g2d.fillOval(eye2X + 1, eye2Y + 1, pupilSize, pupilSize);
    }
    
    private void drawFood(Graphics2D g2d) {
        float pulse = (float)(Math.sin(foodPulse) * 0.2 + 1);
        int size = (int)(BLOCK_SIZE * pulse);
        int offset = (BLOCK_SIZE - size) / 2;
        
        for (int i = 3; i > 0; i--) {
            int glowSize = size + i * 6;
            int glowOffset = (BLOCK_SIZE - glowSize) / 2;
            g2d.setColor(new Color(FOOD_GLOW.getRed(), FOOD_GLOW.getGreen(), FOOD_GLOW.getBlue(), 30 - i * 8));
            g2d.fill(new Ellipse2D.Float(foodX + glowOffset, foodY + glowOffset, glowSize, glowSize));
        }
        
        GradientPaint foodGradient = new GradientPaint(foodX, foodY, FOOD_INNER, foodX + BLOCK_SIZE, foodY + BLOCK_SIZE, FOOD_COLOR);
        g2d.setPaint(foodGradient);
        g2d.fill(new Ellipse2D.Float(foodX + offset, foodY + offset, size, size));
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.fill(new Ellipse2D.Float(foodX + offset + 3, foodY + offset + 3, size / 3, size / 3));
    }
    
    private void drawParticles(Graphics2D g2d) {
        for (Particle p : particles) {
            p.draw(g2d);
        }
    }
    
    private void drawTargets(Graphics2D g2d) {
        for (Target t : targets) {
            t.draw(g2d);
        }
    }
    
    private void drawSlowTargets(Graphics2D g2d) {
        for (SlowTarget st : slowTargets) {
            st.draw(g2d);
        }
    }
    
    private void drawShrinkTargets(Graphics2D g2d) {
        for (ShrinkTarget sht : shrinkTargets) {
            sht.draw(g2d);
        }
    }
    
    private void drawBullets(Graphics2D g2d) {
        for (Bullet b : bullets) {
            b.draw(g2d);
        }
    }
    
    private void drawSlowdownIndicator(Graphics2D g2d) {
        // Draw slowdown timer bar at top
        float progress = (float) slowdownTimer / SLOWDOWN_DURATION;
        int barWidth = (int)(GAME_WIDTH * 0.6f);
        int barHeight = 8;
        int barX = (GAME_WIDTH - barWidth) / 2;
        int barY = 15;
        
        // Background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(barX - 5, barY - 5, barWidth + 10, barHeight + 20, 10, 10);
        
        // Bar background
        g2d.setColor(new Color(50, 50, 100));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);
        
        // Bar fill
        g2d.setColor(SLOW_TARGET_COLOR);
        g2d.fillRoundRect(barX, barY, (int)(barWidth * progress), barHeight, 4, 4);
        
        // Text
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        String text = "SLOWED - " + String.format("%.1f", slowdownTimer / 12.0f) + "s";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, barX + (barWidth - fm.stringWidth(text)) / 2, barY + barHeight + 14);
    }
    
    private void drawStatsPanel(Graphics2D g2d) {
        g2d.setColor(STATS_BG);
        g2d.fillRect(GAME_WIDTH, 0, STATS_WIDTH, GAME_HEIGHT);
        
        g2d.setColor(new Color(0, 255, 150, 100));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(GAME_WIDTH, 0, GAME_WIDTH, GAME_HEIGHT);
        
        int x = GAME_WIDTH + 15;
        int y = 30;
        int lineHeight = 26;
        
        // Title
        g2d.setColor(SNAKE_HEAD_COLOR);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("STATISTICS", x, y);
        y += lineHeight + 5;
        
        // Score
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Score: " + score, x, y);
        y += lineHeight - 5;
        
        g2d.setColor(new Color(200, 200, 200));
        g2d.setFont(new Font("Arial", Font.PLAIN, 13));
        g2d.drawString("High Score: " + highScore, x, y);
        y += lineHeight + 5;
        
        // Divider
        g2d.setColor(new Color(100, 100, 150));
        g2d.drawLine(x, y - 5, GAME_WIDTH + STATS_WIDTH - 15, y - 5);
        y += 5;
        
        // Shooting stats
        g2d.setColor(BULLET_COLOR);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("SHOOTING", x, y);
        y += lineHeight - 5;
        
        g2d.setColor(new Color(180, 180, 180));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Shots: " + totalShots, x, y);
        y += lineHeight - 8;
        g2d.drawString("Hits: " + targetsHit, x, y);
        y += lineHeight - 8;
        
        double accuracy = totalShots > 0 ? (targetsHit * 100.0 / totalShots) : 0;
        Color accColor = accuracy >= 50 ? new Color(100, 255, 100) : accuracy >= 25 ? new Color(255, 200, 50) : new Color(255, 100, 100);
        g2d.setColor(accColor);
        g2d.drawString(String.format("Accuracy: %.1f%%", accuracy), x, y);
        y += lineHeight - 8;
        
        double avgPoints = totalShots > 0 ? (score * 1.0 / totalShots) : 0;
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawString(String.format("Avg Pts/Shot: %.2f", avgPoints), x, y);
        y += lineHeight + 5;
        
        // Divider
        g2d.setColor(new Color(100, 100, 150));
        g2d.drawLine(x, y - 5, GAME_WIDTH + STATS_WIDTH - 15, y - 5);
        y += 5;
        
        // Eating stats
        g2d.setColor(FOOD_COLOR);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("EATING", x, y);
        y += lineHeight - 5;
        
        g2d.setColor(new Color(180, 180, 180));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Food: " + foodEaten, x, y);
        y += lineHeight - 8;
        g2d.drawString("Length: " + snakeLength, x, y);
        y += lineHeight + 5;
        
        // Divider
        g2d.setColor(new Color(100, 100, 150));
        g2d.drawLine(x, y - 5, GAME_WIDTH + STATS_WIDTH - 15, y - 5);
        y += 5;
        
        // Target legend
        g2d.setColor(new Color(255, 255, 255));
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("TARGETS", x, y);
        y += lineHeight - 5;
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        
        // Square targets (dangerous)
        for (TargetType type : TargetType.values()) {
            g2d.setColor(type.color);
            g2d.fillRect(x, y - 9, 10, 10);
            g2d.setColor(new Color(180, 180, 180));
            g2d.drawString(type.name + " +" + type.points, x + 14, y);
            y += 16;
        }
        
        y += 5;
        
        // Slow target (circle)
        g2d.setColor(SLOW_TARGET_COLOR);
        g2d.fillOval(x, y - 9, 10, 10);
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawString("Slow (10s)", x + 14, y);
        y += 18;
        
        // Shrink target (triangle)
        g2d.setColor(SHRINK_TARGET_COLOR);
        int[] txPoints = {x + 5, x, x + 10};
        int[] tyPoints = {y - 9, y + 1, y + 1};
        g2d.fillPolygon(txPoints, tyPoints, 3);
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawString("Shrink (/2)", x + 14, y);
        y += lineHeight + 5;
        
        // Legend explanation
        g2d.setColor(new Color(120, 120, 140));
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        g2d.drawString("Square = Dangerous", x, y);
        y += 14;
        g2d.drawString("Circle/Triangle = Safe", x, y);
        y += lineHeight;
        
        // Controls hint
        g2d.setColor(new Color(100, 100, 120));
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        g2d.drawString("ESC - End Game", x, y);
        y += 14;
        g2d.drawString("Arrows - Move", x, y);
        y += 14;
        g2d.drawString("Space - Shoot", x, y);
    }
    
    private void drawStartScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        
        String title = "FIRE SNAKE";
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        FontMetrics fm = g2d.getFontMetrics();
        int titleX = (GAME_WIDTH - fm.stringWidth(title)) / 2;
        int titleY = GAME_HEIGHT / 3;
        
        for (int i = 10; i > 0; i--) {
            g2d.setColor(new Color(0, 255, 150, 10));
            g2d.drawString(title, titleX - i/2, titleY);
            g2d.drawString(title, titleX + i/2, titleY);
        }
        
        g2d.setColor(SNAKE_HEAD_COLOR);
        g2d.drawString(title, titleX, titleY);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String instruction = "Press ARROW KEY or SPACE to start";
        fm = g2d.getFontMetrics();
        int instX = (GAME_WIDTH - fm.stringWidth(instruction)) / 2;
        
        int alpha = (int)(Math.abs(Math.sin(foodPulse * 2)) * 200 + 55);
        g2d.setColor(new Color(255, 255, 255, alpha));
        g2d.drawString(instruction, instX, GAME_HEIGHT / 2 + 50);
        
        g2d.setColor(new Color(150, 150, 150));
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        String controls = "Arrows - Move | Space - Shoot | ESC - Exit";
        fm = g2d.getFontMetrics();
        g2d.drawString(controls, (GAME_WIDTH - fm.stringWidth(controls)) / 2, GAME_HEIGHT - 80);
        
        g2d.setColor(new Color(255, 100, 100, 200));
        String warning = "Don't collide with SQUARE targets!";
        fm = g2d.getFontMetrics();
        g2d.drawString(warning, (GAME_WIDTH - fm.stringWidth(warning)) / 2, GAME_HEIGHT - 50);
    }
    
    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        
        String title = "GAME OVER";
        g2d.setFont(new Font("Arial", Font.BOLD, 56));
        FontMetrics fm = g2d.getFontMetrics();
        int titleX = (GAME_WIDTH - fm.stringWidth(title)) / 2;
        int titleY = GAME_HEIGHT / 3;
        
        for (int i = 15; i > 0; i--) {
            g2d.setColor(new Color(255, 50, 50, 8));
            g2d.drawString(title, titleX - i/2, titleY);
            g2d.drawString(title, titleX + i/2, titleY);
        }
        
        g2d.setColor(FOOD_COLOR);
        g2d.drawString(title, titleX, titleY);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        String scoreText = "Final Score: " + score;
        fm = g2d.getFontMetrics();
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(scoreText, (GAME_WIDTH - fm.stringWidth(scoreText)) / 2, GAME_HEIGHT / 2);
        
        if (score >= highScore && score > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.setColor(new Color(255, 215, 0));
            String newRecord = "NEW HIGH SCORE!";
            fm = g2d.getFontMetrics();
            g2d.drawString(newRecord, (GAME_WIDTH - fm.stringWidth(newRecord)) / 2, GAME_HEIGHT / 2 + 40);
        }
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.setColor(new Color(200, 200, 200));
        
        String restart = "Press SPACE or ARROW to restart";
        String quit = "Press ESC to exit";
        fm = g2d.getFontMetrics();
        
        g2d.drawString(restart, (GAME_WIDTH - fm.stringWidth(restart)) / 2, GAME_HEIGHT - 100);
        g2d.drawString(quit, (GAME_WIDTH - fm.stringWidth(quit)) / 2, GAME_HEIGHT - 70);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        foodPulse += 0.15f;
        backgroundOffset += 0.02f;
        
        // Update stars
        for (Star star : stars) {
            star.update();
        }
        
        // Update particles
        particles.removeIf(Particle::isDead);
        for (Particle p : particles) {
            p.update();
        }
        
        if (!gameOver && !gameClose && gameStarted) {
            // Update slowdown timer
            if (slowdownTimer > 0) {
                slowdownTimer--;
                if (slowdownTimer == 0) {
                    currentDelay = NORMAL_DELAY;
                    timer.setDelay(currentDelay);
                }
            }
            
            targetSpawnTimer++;
            if (targetSpawnTimer >= TARGET_SPAWN_INTERVAL) {
                spawnTargets();
                targetSpawnTimer = 0;
            }
            
            targets.removeIf(Target::isDead);
            for (Target t : targets) {
                t.update();
            }
            
            slowTargets.removeIf(SlowTarget::isDead);
            for (SlowTarget st : slowTargets) {
                st.update();
            }
            
            shrinkTargets.removeIf(ShrinkTarget::isDead);
            for (ShrinkTarget sht : shrinkTargets) {
                sht.update();
            }
            
            // Update bullets
            for (int i = bullets.size() - 1; i >= 0; i--) {
                Bullet b = bullets.get(i);
                b.update();
                
                if (b.isOutOfBounds()) {
                    bullets.remove(i);
                    continue;
                }
                
                // Check collision with food - GAME OVER!
                float distToFood = (float) Math.sqrt(
                    Math.pow(b.x - (foodX + BLOCK_SIZE / 2), 2) +
                    Math.pow(b.y - (foodY + BLOCK_SIZE / 2), 2)
                );
                if (distToFood < BLOCK_SIZE / 2 + 5) {
                    spawnParticles(foodX, foodY, 15, FOOD_COLOR);
                    endGame();
                    break;
                }
                
                // Check collision with dangerous targets (square)
                boolean bulletHit = false;
                for (int j = targets.size() - 1; j >= 0; j--) {
                    Target t = targets.get(j);
                    float distToTarget = (float) Math.sqrt(
                        Math.pow(b.x - (t.x + BLOCK_SIZE / 2), 2) +
                        Math.pow(b.y - (t.y + BLOCK_SIZE / 2), 2)
                    );
                    if (distToTarget < BLOCK_SIZE / 2 + 5) {
                        spawnParticles(t.x, t.y, 15, t.type.color);
                        score += t.type.points;
                        snakeLength += t.type.points;
                        targetsHit++;
                        targets.remove(j);
                        bullets.remove(i);
                        bulletHit = true;
                        break;
                    }
                }
                
                if (bulletHit) continue;
                
                // Check collision with slow targets (circle)
                for (int j = slowTargets.size() - 1; j >= 0; j--) {
                    SlowTarget st = slowTargets.get(j);
                    float distToTarget = (float) Math.sqrt(
                        Math.pow(b.x - (st.x + BLOCK_SIZE / 2), 2) +
                        Math.pow(b.y - (st.y + BLOCK_SIZE / 2), 2)
                    );
                    if (distToTarget < BLOCK_SIZE / 2 + 5) {
                        spawnParticles(st.x, st.y, 15, SLOW_TARGET_COLOR);
                        activateSlowdown();
                        targetsHit++;
                        slowTargets.remove(j);
                        bullets.remove(i);
                        bulletHit = true;
                        break;
                    }
                }
                
                if (bulletHit) continue;
                
                // Check collision with shrink targets (triangle)
                for (int j = shrinkTargets.size() - 1; j >= 0; j--) {
                    ShrinkTarget sht = shrinkTargets.get(j);
                    float distToTarget = (float) Math.sqrt(
                        Math.pow(b.x - (sht.x + BLOCK_SIZE / 2), 2) +
                        Math.pow(b.y - (sht.y + BLOCK_SIZE / 2), 2)
                    );
                    if (distToTarget < BLOCK_SIZE / 2 + 5) {
                        spawnParticles(sht.x, sht.y, 15, SHRINK_TARGET_COLOR);
                        // Shrink snake by half (minimum length 1)
                        snakeLength = Math.max(1, snakeLength / 2);
                        // Also remove excess segments from list
                        while (snakeList.size() > snakeLength) {
                            snakeList.remove(0);
                        }
                        targetsHit++;
                        shrinkTargets.remove(j);
                        bullets.remove(i);
                        break;
                    }
                }
            }
            
            // Process direction
            if (!directionQueue.isEmpty()) {
                int[] nextDir = directionQueue.peek();
                if (isValidDirectionChange(nextDir[0], nextDir[1])) {
                    x1Change = nextDir[0];
                    y1Change = nextDir[1];
                }
                directionQueue.poll();
            }
            
            // Check wall collision
            if (x1 >= GAME_WIDTH || x1 < 0 || y1 >= GAME_HEIGHT || y1 < 0) {
                endGame();
            }
            
            if (!gameClose) {
                x1 += x1Change;
                y1 += y1Change;
                
                snakeList.add(new int[]{x1, y1});
                
                if (snakeList.size() > snakeLength) {
                    snakeList.remove(0);
                }
                
                // Check food collision
                if (x1 == foodX && y1 == foodY) {
                    spawnParticles(foodX, foodY, 15, null);
                    spawnFood();
                    snakeLength++;
                    score++;
                    foodEaten++;
                }
                
                // Check dangerous target collision (square) - GAME OVER!
                for (Target t : targets) {
                    if (x1 == t.x && y1 == t.y) {
                        spawnParticles(t.x, t.y, 20, t.type.color);
                        endGame();
                        break;
                    }
                }
                
                // Slow targets are safe to pass through (no collision damage)
                
                // Check self collision
                for (int i = 0; i < snakeList.size() - 1; i++) {
                    int[] segment = snakeList.get(i);
                    if (segment[0] == x1 && segment[1] == y1) {
                        endGame();
                        break;
                    }
                }
            }
        }
        
        repaint();
    }
    
    private void endGame() {
        gameClose = true;
        if (score > highScore) {
            highScore = score;
        }
    }
    
    private boolean isValidDirectionChange(int newXChange, int newYChange) {
        return true;
    }
    
    private void queueDirection(int xChange, int yChange) {
        if (directionQueue.size() >= MAX_QUEUE_SIZE) {
            return;
        }
        
        int lastXChange = x1Change;
        int lastYChange = y1Change;
        
        if (!directionQueue.isEmpty()) {
            int[] last = null;
            for (int[] dir : directionQueue) {
                last = dir;
            }
            if (last != null) {
                lastXChange = last[0];
                lastYChange = last[1];
            }
        }
        
        if (xChange == lastXChange && yChange == lastYChange) return;
        
        directionQueue.add(new int[]{xChange, yChange});
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        if (key == KeyEvent.VK_ESCAPE) {
            if (!gameStarted || gameClose) {
                System.exit(0);
            } else {
                endGame();
            }
            return;
        }
        
        if (gameClose) {
            boolean isArrowKey = key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT || 
                                 key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN;
            if (key == KeyEvent.VK_SPACE || isArrowKey) {
                initGame();
                if (isArrowKey) {
                    gameStarted = true;
                    if (key == KeyEvent.VK_LEFT) queueDirection(-BLOCK_SIZE, 0);
                    else if (key == KeyEvent.VK_RIGHT) queueDirection(BLOCK_SIZE, 0);
                    else if (key == KeyEvent.VK_UP) queueDirection(0, -BLOCK_SIZE);
                    else if (key == KeyEvent.VK_DOWN) queueDirection(0, BLOCK_SIZE);
                }
            }
        } else {
            boolean isArrowKey = key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT || 
                                 key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN;
            
            if (!gameStarted && (isArrowKey || key == KeyEvent.VK_SPACE)) {
                gameStarted = true;
            }
            
            if (key == KeyEvent.VK_LEFT) {
                queueDirection(-BLOCK_SIZE, 0);
            } else if (key == KeyEvent.VK_RIGHT) {
                queueDirection(BLOCK_SIZE, 0);
            } else if (key == KeyEvent.VK_UP) {
                queueDirection(0, -BLOCK_SIZE);
            } else if (key == KeyEvent.VK_DOWN) {
                queueDirection(0, BLOCK_SIZE);
            } else if (key == KeyEvent.VK_SPACE) {
                shoot();
            }
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {}
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Fire Snake");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new FireSnakeGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
