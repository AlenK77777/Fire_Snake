package com.firesnake;

import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
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
    private static final int FAST_DELAY = 1000 / 24;   // 24 FPS (2x faster)
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
    
    // Speed target color (orange/yellow)
    private static final Color SPEED_TARGET_COLOR = new Color(255, 150, 0);
    private static final Color SPEED_TARGET_INNER = new Color(255, 220, 100);
    
    // Snake speed color (when sped up)
    private static final Color SNAKE_FAST_COLOR = new Color(255, 180, 50); // Orange tint when fast
    
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
    private int globalHighScore; // Saved to file, persists between sessions
    private int sessionHighScore; // Only for current session
    private boolean gameOver;
    private boolean gameClose;
    private boolean gameStarted;
    private boolean newGlobalRecord = false; // Flag for new global record
    private boolean newSessionRecord = false; // Flag for new session record
    private boolean globalRecordAnnounced = false; // Prevent repeated announcements
    private boolean sessionRecordAnnounced = false;
    
    // High score file
    private static final String HIGH_SCORE_FILE = "fire_snake_highscore.dat";
    
    // Auto-fire when holding space
    private boolean spacePressed = false;
    private int autoFireCooldown = 0;
    private static final int AUTO_FIRE_DELAY = 4; // Fire every 4 frames (~3 shots per second at 12 FPS)
    
    // Slowdown effect
    private int slowdownTimer = 0;
    private static final int SLOWDOWN_DURATION = 120; // 10 seconds at 12 FPS
    
    // Speedup effect
    private int speedupTimer = 0;
    private static final int SPEEDUP_DURATION = 240; // 10 seconds at 24 FPS (double speed)
    
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
    private ArrayList<SpeedTarget> speedTargets;
    private int targetSpawnTimer = 0;
    private static final int TARGET_SPAWN_INTERVAL = 60;
    private static final int BULLET_SPEED = 50;
    
    private Timer timer;
    private Random random;
    
    // Retro Sound Engine for 8-bit style sounds
    private RetroSoundEngine soundEngine;
    
    // Sound/Music mute state
    private boolean soundMuted = false;
    
    // Background music engine
    private MusicEngine musicEngine;
    
    // Track snake length for music tempo increase (every 10 segments)
    private int tempoLevel = 0;
    
    // Food timer (10 seconds to eat food or game over)
    private int foodTimer = 0;
    private static final int FOOD_TIME_LIMIT = 120; // 10 seconds at 12 FPS
    
    // Inner class for generating retro 8-bit style sounds
    private class RetroSoundEngine {
        private static final int SAMPLE_RATE = 44100;
        
        RetroSoundEngine() {}
        
        // Generate square wave (classic 8-bit sound)
        private byte[] generateSquareWave(double frequency, int durationMs, double volume) {
            int numSamples = (int)(SAMPLE_RATE * durationMs / 1000.0);
            byte[] buffer = new byte[numSamples];
            double period = SAMPLE_RATE / frequency;
            
            for (int i = 0; i < numSamples; i++) {
                double phase = (i % period) / period;
                byte value = (byte)(phase < 0.5 ? 127 * volume : -127 * volume);
                // Apply simple envelope for decay
                double envelope = 1.0 - ((double)i / numSamples);
                buffer[i] = (byte)(value * envelope);
            }
            return buffer;
        }
        
        // Generate noise (for explosions)
        private byte[] generateNoise(int durationMs, double volume) {
            int numSamples = (int)(SAMPLE_RATE * durationMs / 1000.0);
            byte[] buffer = new byte[numSamples];
            Random noiseRandom = new Random();
            
            for (int i = 0; i < numSamples; i++) {
                // Apply decay envelope for explosion effect
                double envelope = 1.0 - ((double)i / numSamples);
                envelope = Math.pow(envelope, 0.5); // Faster decay
                buffer[i] = (byte)(noiseRandom.nextInt(256) - 128 * volume * envelope);
            }
            return buffer;
        }
        
        // Generate frequency sweep (for shooting sound)
        private byte[] generateSweep(double startFreq, double endFreq, int durationMs, double volume) {
            int numSamples = (int)(SAMPLE_RATE * durationMs / 1000.0);
            byte[] buffer = new byte[numSamples];
            
            double phase = 0;
            for (int i = 0; i < numSamples; i++) {
                double progress = (double)i / numSamples;
                double currentFreq = startFreq + (endFreq - startFreq) * progress;
                double envelope = 1.0 - progress;
                
                phase += 2 * Math.PI * currentFreq / SAMPLE_RATE;
                // Square wave
                double value = Math.sin(phase) > 0 ? 127 : -127;
                buffer[i] = (byte)(value * volume * envelope);
            }
            return buffer;
        }
        
        private void playSound(byte[] buffer) {
            if (soundMuted) return;
            new Thread(() -> {
                try {
                    AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(format);
                    line.start();
                    line.write(buffer, 0, buffer.length);
                    line.drain();
                    line.close();
                } catch (Exception e) {
                    // Silently ignore audio errors
                }
            }).start();
        }
        
        // Shooting sound - high-pitched sweep down
        void playShoot() {
            if (soundMuted) return;
            byte[] sound = generateSweep(1200, 400, 80, 0.5);
            playSound(sound);
        }
        
        // Explosion sound - noise burst with bass
        void playExplosion() {
            if (soundMuted) return;
            int duration = 150;
            byte[] noise = generateNoise(duration, 0.6);
            byte[] bass = generateSquareWave(80, duration, 0.4);
            
            // Mix noise and bass
            byte[] mixed = new byte[noise.length];
            for (int i = 0; i < noise.length && i < bass.length; i++) {
                mixed[i] = (byte)Math.max(-127, Math.min(127, (noise[i] + bass[i]) / 2));
            }
            playSound(mixed);
        }
        
        // Collision/hit sound - short noise burst
        void playHit() {
            if (soundMuted) return;
            byte[] sound = generateNoise(60, 0.4);
            playSound(sound);
        }
        
        // Power-up sound (for slow/speed effects)
        void playPowerUp() {
            if (soundMuted) return;
            byte[] sound = generateSweep(300, 1000, 150, 0.4);
            playSound(sound);
        }
        
        // Shrink sound - descending tone
        void playShrink() {
            if (soundMuted) return;
            byte[] sound = generateSweep(800, 200, 200, 0.5);
            playSound(sound);
        }
        
        // Game over sound
        void playGameOver() {
            if (soundMuted) return;
            new Thread(() -> {
                try {
                    // Play descending tones
                    byte[] tone1 = generateSquareWave(400, 150, 0.5);
                    byte[] tone2 = generateSquareWave(300, 150, 0.5);
                    byte[] tone3 = generateSquareWave(200, 300, 0.5);
                    
                    AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(format);
                    line.start();
                    line.write(tone1, 0, tone1.length);
                    line.write(tone2, 0, tone2.length);
                    line.write(tone3, 0, tone3.length);
                    line.drain();
                    line.close();
                } catch (Exception e) {
                    // Silently ignore audio errors
                }
            }).start();
        }
        
        // Food eaten sound - quick ascending blip
        void playEat() {
            if (soundMuted) return;
            byte[] sound = generateSweep(500, 900, 50, 0.3);
            playSound(sound);
        }
        
        // Speed boost sound
        void playSpeedUp() {
            if (soundMuted) return;
            byte[] sound = generateSweep(400, 1500, 200, 0.4);
            playSound(sound);
        }
        
        // Victory sound - new high score!
        // Session record sound - short ascending fanfare
        void playSessionRecord() {
            if (soundMuted) return;
            new Thread(() -> {
                try {
                    // Short victory jingle for session record
                    byte[] tone1 = generateSquareWave(440.00, 80, 0.5); // A4
                    byte[] tone2 = generateSquareWave(554.37, 80, 0.5); // C#5
                    byte[] tone3 = generateSquareWave(659.25, 150, 0.55); // E5
                    
                    AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(format);
                    line.start();
                    line.write(tone1, 0, tone1.length);
                    line.write(tone2, 0, tone2.length);
                    line.write(tone3, 0, tone3.length);
                    line.drain();
                    line.close();
                } catch (Exception e) {
                    // Silently ignore
                }
            }).start();
        }
        
        // Global record sound - epic triumphant fanfare (longer, more impressive)
        void playGlobalRecord() {
            if (soundMuted) return;
            new Thread(() -> {
                try {
                    // Epic victory fanfare for GLOBAL record - "You beat the record!"
                    byte[] tone1 = generateSquareWave(523.25, 100, 0.5); // C5
                    byte[] tone2 = generateSquareWave(659.25, 100, 0.5); // E5
                    byte[] tone3 = generateSquareWave(783.99, 100, 0.5); // G5
                    byte[] pause = new byte[(int)(SAMPLE_RATE * 0.05)]; // Short pause
                    byte[] tone4 = generateSquareWave(783.99, 100, 0.55); // G5
                    byte[] tone5 = generateSquareWave(880.00, 100, 0.55); // A5
                    byte[] tone6 = generateSquareWave(1046.50, 400, 0.6); // C6 (long final note)
                    
                    AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(format);
                    line.start();
                    line.write(tone1, 0, tone1.length);
                    line.write(tone2, 0, tone2.length);
                    line.write(tone3, 0, tone3.length);
                    line.write(pause, 0, pause.length);
                    line.write(tone4, 0, tone4.length);
                    line.write(tone5, 0, tone5.length);
                    line.write(tone6, 0, tone6.length);
                    line.drain();
                    line.close();
                } catch (Exception e) {
                    // Silently ignore
                }
            }).start();
        }
    }
    
    // Background music engine - generates looping 8-bit style music
    private class MusicEngine {
        private static final int SAMPLE_RATE = 44100;
        private volatile boolean playing = false;
        private volatile boolean isMenuMusic = true;
        private Thread musicThread;
        private SourceDataLine currentLine;
        private volatile double tempoMultiplier = 1.0; // Speed multiplier for game music
        
        MusicEngine() {}
        
        void startMenuMusic() {
            stopMusic();
            isMenuMusic = true;
            playing = true;
            tempoMultiplier = 1.0;
            musicThread = new Thread(this::playMenuLoop);
            musicThread.start();
        }
        
        void startGameMusic() {
            stopMusic();
            isMenuMusic = false;
            playing = true;
            tempoMultiplier = 1.0;
            musicThread = new Thread(this::playGameLoop);
            musicThread.start();
        }
        
        void setTempoMultiplier(double multiplier) {
            this.tempoMultiplier = Math.min(3.0, multiplier); // Cap at 3x speed
        }
        
        double getTempoMultiplier() {
            return tempoMultiplier;
        }
        
        void stopMusic() {
            playing = false;
            if (currentLine != null) {
                currentLine.stop();
                currentLine.close();
                currentLine = null;
            }
            if (musicThread != null) {
                musicThread.interrupt();
                musicThread = null;
            }
        }
        
        // Generate a note with proper envelope (attack, decay)
        private byte[] generateNote(double frequency, int durationMs, double volume, String waveType) {
            int numSamples = (int)(SAMPLE_RATE * durationMs / 1000.0);
            byte[] buffer = new byte[numSamples];
            double period = SAMPLE_RATE / frequency;
            
            int attackSamples = Math.min(numSamples / 10, (int)(SAMPLE_RATE * 0.01));
            int releaseSamples = numSamples / 4;
            
            for (int i = 0; i < numSamples; i++) {
                double phase = (i % period) / period;
                double value = 0;
                
                switch (waveType) {
                    case "square":
                        value = phase < 0.5 ? 1 : -1;
                        // Soften square wave a bit
                        value *= 0.7;
                        break;
                    case "triangle":
                        value = phase < 0.5 ? (4 * phase - 1) : (3 - 4 * phase);
                        break;
                    case "pulse25":
                        value = phase < 0.25 ? 1 : -1;
                        value *= 0.6;
                        break;
                    default:
                        value = Math.sin(2 * Math.PI * phase);
                }
                
                // Envelope
                double env = 1.0;
                if (i < attackSamples) {
                    env = (double) i / attackSamples;
                } else if (i > numSamples - releaseSamples) {
                    env = (double)(numSamples - i) / releaseSamples;
                }
                
                buffer[i] = (byte)(value * 50 * volume * env);
            }
            return buffer;
        }
        
        private void playMenuLoop() {
            try {
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                currentLine = (SourceDataLine) AudioSystem.getLine(info);
                currentLine.open(format);
                currentLine.start();
                
                // Menu music - calm, mysterious melody (like classic game title screens)
                // E minor pentatonic
                double E3 = 164.81, G3 = 196.00, A3 = 220.00, B3 = 246.94, D4 = 293.66, E4 = 329.63;
                
                while (playing) {
                    if (soundMuted) {
                        Thread.sleep(100);
                        continue;
                    }
                    
                    // Gentle melody pattern
                    int noteDur = 300;
                    
                    // Phrase 1
                    playNote(E3, noteDur, 0.4, "triangle");
                    playNote(G3, noteDur, 0.35, "triangle");
                    playNote(A3, noteDur, 0.4, "triangle");
                    playNote(G3, noteDur, 0.35, "triangle");
                    
                    // Phrase 2
                    playNote(B3, noteDur, 0.4, "triangle");
                    playNote(A3, noteDur, 0.35, "triangle");
                    playNote(G3, noteDur, 0.4, "triangle");
                    playNote(E3, noteDur * 2, 0.35, "triangle");
                    
                    // Phrase 3 - higher
                    playNote(D4, noteDur, 0.35, "triangle");
                    playNote(B3, noteDur, 0.3, "triangle");
                    playNote(A3, noteDur, 0.35, "triangle");
                    playNote(G3, noteDur * 2, 0.3, "triangle");
                    
                    // Rest
                    Thread.sleep(400);
                }
                
                currentLine.drain();
                currentLine.close();
            } catch (Exception e) {
                // Silently ignore
            }
        }
        
        private void playNote(double freq, int duration, double volume, String waveType) {
            if (!playing || soundMuted || currentLine == null) return;
            byte[] note = generateNote(freq, duration, volume, waveType);
            currentLine.write(note, 0, note.length);
        }
        
        private void playGameLoop() {
            try {
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                currentLine = (SourceDataLine) AudioSystem.getLine(info);
                currentLine.open(format);
                currentLine.start();
                
                // Game music - catchy, upbeat melody inspired by classic games
                // C major / A minor scale for a fun feel
                double C4 = 261.63, D4 = 293.66, E4 = 329.63, F4 = 349.23;
                double G4 = 392.00, A4 = 440.00, B4 = 493.88, C5 = 523.25;
                double C3 = 130.81, E3 = 164.81, G3 = 196.00, A3 = 220.00;
                
                int measure = 0;
                
                while (playing) {
                    if (soundMuted) {
                        Thread.sleep(100);
                        continue;
                    }
                    
                    // Base note duration adjusted by tempo
                    int baseNote = (int)(150 / tempoMultiplier);
                    int halfNote = (int)(75 / tempoMultiplier);
                    int longNote = (int)(300 / tempoMultiplier);
                    
                    measure++;
                    
                    // Main catchy melody (like classic arcade games)
                    switch (measure % 4) {
                        case 0:
                            // Rising phrase
                            playGameNote(C4, baseNote, 0.4, "pulse25");
                            playGameNote(E4, halfNote, 0.35, "pulse25");
                            playGameNote(G4, baseNote, 0.4, "pulse25");
                            playGameNote(C5, longNote, 0.45, "pulse25");
                            // Bass
                            playGameNote(C3, baseNote, 0.5, "triangle");
                            playGameNote(G3, baseNote, 0.45, "triangle");
                            break;
                            
                        case 1:
                            // Descending phrase  
                            playGameNote(B4, halfNote, 0.35, "pulse25");
                            playGameNote(G4, halfNote, 0.35, "pulse25");
                            playGameNote(E4, baseNote, 0.4, "pulse25");
                            playGameNote(D4, baseNote, 0.4, "pulse25");
                            playGameNote(C4, longNote, 0.4, "pulse25");
                            // Bass
                            playGameNote(E3, baseNote, 0.5, "triangle");
                            playGameNote(C3, baseNote, 0.45, "triangle");
                            break;
                            
                        case 2:
                            // Bounce pattern
                            playGameNote(A4, halfNote, 0.4, "pulse25");
                            playGameNote(E4, halfNote, 0.35, "pulse25");
                            playGameNote(A4, halfNote, 0.4, "pulse25");
                            playGameNote(E4, halfNote, 0.35, "pulse25");
                            playGameNote(G4, baseNote, 0.4, "pulse25");
                            playGameNote(F4, baseNote, 0.35, "pulse25");
                            playGameNote(E4, baseNote, 0.4, "pulse25");
                            // Bass
                            playGameNote(A3, baseNote, 0.5, "triangle");
                            playGameNote(E3, baseNote, 0.45, "triangle");
                            break;
                            
                        case 3:
                            // Resolution
                            playGameNote(D4, baseNote, 0.4, "pulse25");
                            playGameNote(E4, baseNote, 0.4, "pulse25");
                            playGameNote(F4, halfNote, 0.35, "pulse25");
                            playGameNote(G4, longNote, 0.45, "pulse25");
                            // Short rest
                            Thread.sleep((int)(100 / tempoMultiplier));
                            // Final note
                            playGameNote(C4, longNote, 0.4, "pulse25");
                            // Bass
                            playGameNote(G3, baseNote, 0.5, "triangle");
                            playGameNote(C3, longNote, 0.5, "triangle");
                            break;
                    }
                    
                    // Small pause between measures
                    Thread.sleep((int)(50 / tempoMultiplier));
                }
                
                currentLine.drain();
                currentLine.close();
            } catch (Exception e) {
                // Silently ignore
            }
        }
        
        private void playGameNote(double freq, int duration, double volume, String waveType) {
            if (!playing || soundMuted || currentLine == null) return;
            byte[] note = generateNote(freq, duration, volume, waveType);
            currentLine.write(note, 0, note.length);
        }
    }
    
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
        int gridSize; // 1, 2, or 3 (in cells)
        float lifetime;
        float maxLifetime;
        float pulse = 0;
        TargetType type;
        int spawnDelay; // 3 second spawn delay where target is inactive and blinking
        static final int SPAWN_DELAY_DURATION = 36; // 3 seconds at 12 FPS
        
        Target(int x, int y, TargetType type) {
            this(x, y, type, 1);
        }
        
        Target(int x, int y, TargetType type, int gridSize) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.gridSize = gridSize;
            float baseLife = 60 + random.nextInt(61);
            this.maxLifetime = baseLife / (1 + type.ordinal() * 0.2f);
            this.lifetime = maxLifetime;
            this.spawnDelay = SPAWN_DELAY_DURATION; // Start with spawn delay
        }
        
        // Check if a grid cell is occupied by this target
        boolean occupiesCell(int cellX, int cellY) {
            for (int dx = 0; dx < gridSize; dx++) {
                for (int dy = 0; dy < gridSize; dy++) {
                    if (x + dx * BLOCK_SIZE == cellX && y + dy * BLOCK_SIZE == cellY) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        // Get pixel size
        int getPixelSize() {
            return gridSize * BLOCK_SIZE;
        }
        
        void update() {
            if (spawnDelay > 0) {
                spawnDelay--;
            } else {
                lifetime--;
            }
            pulse += 0.2f + type.ordinal() * 0.05f;
        }
        
        boolean isDead() {
            return lifetime <= 0;
        }
        
        boolean isActive() {
            return spawnDelay <= 0; // Only dangerous when spawn delay is over
        }
        
        void draw(Graphics2D g2d) {
            float alpha = Math.min(1.0f, lifetime / 30.0f);
            
            // Blink effect during spawn delay (inactive period)
            if (spawnDelay > 0) {
                // Fast blinking - visible every other few frames
                if ((spawnDelay / 3) % 2 == 0) {
                    alpha *= 0.3f; // Very transparent when blinking off
                } else {
                    alpha *= 0.7f; // Semi-transparent when blinking on
                }
            }
            
            int totalSize = gridSize * BLOCK_SIZE;
            float pulseScale = (float)(Math.sin(pulse) * 0.05 + 1);
            int size = (int)(totalSize * pulseScale);
            int offset = (totalSize - size) / 2;
            
            // Draw outer glow (square)
            for (int i = 3; i > 0; i--) {
                int glowSize = size + i * 6;
                int glowOffset = (totalSize - glowSize) / 2;
                g2d.setColor(new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), (int)((30 - i * 8) * alpha)));
                g2d.fill(new RoundRectangle2D.Float(x + glowOffset, y + glowOffset, glowSize, glowSize, 4, 4));
            }
            
            // Draw target SQUARE
            GradientPaint targetGradient = new GradientPaint(
                x, y, new Color(type.innerColor.getRed(), type.innerColor.getGreen(), type.innerColor.getBlue(), (int)(255 * alpha)),
                x + totalSize, y + totalSize, new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), (int)(255 * alpha))
            );
            g2d.setPaint(targetGradient);
            g2d.fill(new RoundRectangle2D.Float(x + offset, y + offset, size, size, 6, 6));
            
            // Draw X pattern (danger indicator) - scaled for larger targets
            g2d.setColor(new Color(255, 255, 255, (int)(200 * alpha)));
            g2d.setStroke(new BasicStroke(2 + gridSize - 1));
            int centerX = x + totalSize / 2;
            int centerY = y + totalSize / 2;
            int crossSize = size / 4;
            g2d.drawLine(centerX - crossSize, centerY - crossSize, centerX + crossSize, centerY + crossSize);
            g2d.drawLine(centerX + crossSize, centerY - crossSize, centerX - crossSize, centerY + crossSize);
            
            // Draw border
            g2d.setColor(new Color(255, 255, 255, (int)(100 * alpha)));
            g2d.draw(new RoundRectangle2D.Float(x + offset, y + offset, size, size, 6, 6));
            
            // Draw points indicator
            g2d.setColor(new Color(255, 255, 255, (int)(180 * alpha)));
            g2d.setFont(new Font("Arial", Font.BOLD, 10 + gridSize * 2));
            String pts = "+" + type.points;
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(pts, centerX - fm.stringWidth(pts)/2, y - 2);
            
            // Draw lifetime bar
            float lifePercent = lifetime / maxLifetime;
            int barWidth = (int)(totalSize * lifePercent);
            g2d.setColor(new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), (int)(150 * alpha)));
            g2d.fillRect(x, y + totalSize + 2, barWidth, 3);
        }
    }
    
    // Slow Target class - CIRCLE shape (safe to pass, slows snake when shot)
    private class SlowTarget {
        int x, y;
        int gridSize; // 1, 2, or 3
        float lifetime;
        float maxLifetime;
        float pulse = 0;
        
        SlowTarget(int x, int y) {
            this(x, y, 1);
        }
        
        SlowTarget(int x, int y, int gridSize) {
            this.x = x;
            this.y = y;
            this.gridSize = gridSize;
            this.maxLifetime = 80 + random.nextInt(40);
            this.lifetime = maxLifetime;
        }
        
        boolean occupiesCell(int cellX, int cellY) {
            for (int dx = 0; dx < gridSize; dx++) {
                for (int dy = 0; dy < gridSize; dy++) {
                    if (x + dx * BLOCK_SIZE == cellX && y + dy * BLOCK_SIZE == cellY) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        int getPixelSize() {
            return gridSize * BLOCK_SIZE;
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
            int totalSize = gridSize * BLOCK_SIZE;
            float pulseScale = (float)(Math.sin(pulse) * 0.1 + 1);
            int size = (int)(totalSize * pulseScale);
            int offset = (totalSize - size) / 2;
            
            // Draw outer glow (circle)
            for (int i = 3; i > 0; i--) {
                int glowSize = size + i * 6;
                int glowOffset = (totalSize - glowSize) / 2;
                g2d.setColor(new Color(SLOW_TARGET_COLOR.getRed(), SLOW_TARGET_COLOR.getGreen(), SLOW_TARGET_COLOR.getBlue(), (int)((30 - i * 8) * alpha)));
                g2d.fill(new Ellipse2D.Float(x + glowOffset, y + glowOffset, glowSize, glowSize));
            }
            
            // Draw target CIRCLE
            GradientPaint targetGradient = new GradientPaint(
                x, y, new Color(SLOW_TARGET_INNER.getRed(), SLOW_TARGET_INNER.getGreen(), SLOW_TARGET_INNER.getBlue(), (int)(255 * alpha)),
                x + totalSize, y + totalSize, new Color(SLOW_TARGET_COLOR.getRed(), SLOW_TARGET_COLOR.getGreen(), SLOW_TARGET_COLOR.getBlue(), (int)(255 * alpha))
            );
            g2d.setPaint(targetGradient);
            g2d.fill(new Ellipse2D.Float(x + offset, y + offset, size, size));
            
            // Draw slow icon (hourglass-like) - scaled
            g2d.setColor(new Color(255, 255, 255, (int)(200 * alpha)));
            g2d.setStroke(new BasicStroke(1 + gridSize));
            int centerX = x + totalSize / 2;
            int centerY = y + totalSize / 2;
            int iconSize = 4 * gridSize;
            g2d.drawLine(centerX - iconSize, centerY - iconSize, centerX + iconSize, centerY - iconSize);
            g2d.drawLine(centerX - iconSize, centerY + iconSize, centerX + iconSize, centerY + iconSize);
            g2d.drawLine(centerX - iconSize, centerY - iconSize, centerX, centerY);
            g2d.drawLine(centerX + iconSize, centerY - iconSize, centerX, centerY);
            g2d.drawLine(centerX - iconSize, centerY + iconSize, centerX, centerY);
            g2d.drawLine(centerX + iconSize, centerY + iconSize, centerX, centerY);
            
            // Draw "SLOW" text
            g2d.setColor(new Color(255, 255, 255, (int)(150 * alpha)));
            g2d.setFont(new Font("Arial", Font.BOLD, 8 + gridSize * 2));
            g2d.drawString("SLOW", x - 2, y - 2);
            
            // Draw lifetime bar
            float lifePercent = lifetime / maxLifetime;
            int barWidth = (int)(totalSize * lifePercent);
            g2d.setColor(new Color(SLOW_TARGET_COLOR.getRed(), SLOW_TARGET_COLOR.getGreen(), SLOW_TARGET_COLOR.getBlue(), (int)(150 * alpha)));
            g2d.fillRect(x, y + totalSize + 2, barWidth, 3);
        }
    }
    
    // Shrink Target class - TRIANGLE shape (safe to pass, shrinks snake by half when shot)
    private class ShrinkTarget {
        int x, y;
        int gridSize; // 1, 2, or 3
        float lifetime;
        float maxLifetime;
        float pulse = 0;
        
        ShrinkTarget(int x, int y) {
            this(x, y, 1);
        }
        
        ShrinkTarget(int x, int y, int gridSize) {
            this.x = x;
            this.y = y;
            this.gridSize = gridSize;
            this.maxLifetime = 70 + random.nextInt(50);
            this.lifetime = maxLifetime;
        }
        
        boolean occupiesCell(int cellX, int cellY) {
            for (int dx = 0; dx < gridSize; dx++) {
                for (int dy = 0; dy < gridSize; dy++) {
                    if (x + dx * BLOCK_SIZE == cellX && y + dy * BLOCK_SIZE == cellY) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        int getPixelSize() {
            return gridSize * BLOCK_SIZE;
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
            int totalSize = gridSize * BLOCK_SIZE;
            float pulseScale = (float)(Math.sin(pulse) * 0.1 + 1);
            int size = (int)(totalSize * pulseScale);
            
            // Draw outer glow (triangle shape)
            int centerX = x + totalSize / 2;
            int centerY = y + totalSize / 2;
            
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
            
            // Draw down arrow inside (shrink indicator) - scaled
            g2d.setColor(new Color(255, 255, 255, (int)(200 * alpha)));
            g2d.setStroke(new BasicStroke(1 + gridSize));
            int arrowSize = 3 * gridSize;
            g2d.drawLine(centerX, centerY - arrowSize, centerX, centerY + arrowSize + 1);
            g2d.drawLine(centerX - arrowSize, centerY + 1, centerX, centerY + arrowSize + 1);
            g2d.drawLine(centerX + arrowSize, centerY + 1, centerX, centerY + arrowSize + 1);
            
            // Draw "/2" text
            g2d.setColor(new Color(255, 255, 255, (int)(150 * alpha)));
            g2d.setFont(new Font("Arial", Font.BOLD, 8 + gridSize * 2));
            g2d.drawString("/2", x + 5, y - 2);
            
            // Draw lifetime bar
            float lifePercent = lifetime / maxLifetime;
            int barWidth = (int)(totalSize * lifePercent);
            g2d.setColor(new Color(SHRINK_TARGET_COLOR.getRed(), SHRINK_TARGET_COLOR.getGreen(), SHRINK_TARGET_COLOR.getBlue(), (int)(150 * alpha)));
            g2d.fillRect(x, y + totalSize + 2, barWidth, 3);
        }
    }
    
    // Speed Target class - DIAMOND shape (safe to pass, speeds up snake 2x for 10 seconds when shot)
    private class SpeedTarget {
        int x, y;
        int gridSize; // 1, 2, or 3
        float lifetime;
        float maxLifetime;
        float pulse = 0;
        
        SpeedTarget(int x, int y) {
            this(x, y, 1);
        }
        
        SpeedTarget(int x, int y, int gridSize) {
            this.x = x;
            this.y = y;
            this.gridSize = gridSize;
            this.maxLifetime = 70 + random.nextInt(50);
            this.lifetime = maxLifetime;
        }
        
        boolean occupiesCell(int cellX, int cellY) {
            for (int dx = 0; dx < gridSize; dx++) {
                for (int dy = 0; dy < gridSize; dy++) {
                    if (x + dx * BLOCK_SIZE == cellX && y + dy * BLOCK_SIZE == cellY) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        int getPixelSize() {
            return gridSize * BLOCK_SIZE;
        }
        
        void update() {
            lifetime--;
            pulse += 0.2f;
        }
        
        boolean isDead() {
            return lifetime <= 0;
        }
        
        void draw(Graphics2D g2d) {
            float alpha = Math.min(1.0f, lifetime / 30.0f);
            int totalSize = gridSize * BLOCK_SIZE;
            float pulseScale = (float)(Math.sin(pulse) * 0.1 + 1);
            int size = (int)(totalSize * pulseScale);
            
            int centerX = x + totalSize / 2;
            int centerY = y + totalSize / 2;
            int halfSize = size / 2;
            
            // Draw outer glow (diamond shape)
            for (int i = 3; i > 0; i--) {
                int glowSize = halfSize + i * 3;
                int[] xPoints = {centerX, centerX + glowSize, centerX, centerX - glowSize};
                int[] yPoints = {centerY - glowSize, centerY, centerY + glowSize, centerY};
                g2d.setColor(new Color(SPEED_TARGET_COLOR.getRed(), SPEED_TARGET_COLOR.getGreen(), SPEED_TARGET_COLOR.getBlue(), (int)((25 - i * 6) * alpha)));
                g2d.fillPolygon(xPoints, yPoints, 4);
            }
            
            // Draw target DIAMOND
            int[] xPoints = {centerX, centerX + halfSize, centerX, centerX - halfSize};
            int[] yPoints = {centerY - halfSize, centerY, centerY + halfSize, centerY};
            
            GradientPaint targetGradient = new GradientPaint(
                centerX, centerY - halfSize, new Color(SPEED_TARGET_INNER.getRed(), SPEED_TARGET_INNER.getGreen(), SPEED_TARGET_INNER.getBlue(), (int)(255 * alpha)),
                centerX, centerY + halfSize, new Color(SPEED_TARGET_COLOR.getRed(), SPEED_TARGET_COLOR.getGreen(), SPEED_TARGET_COLOR.getBlue(), (int)(255 * alpha))
            );
            g2d.setPaint(targetGradient);
            g2d.fillPolygon(xPoints, yPoints, 4);
            
            // Draw lightning bolt inside (speed indicator) - scaled
            g2d.setColor(new Color(255, 255, 255, (int)(220 * alpha)));
            g2d.setStroke(new BasicStroke(1 + gridSize));
            int boltSize = 2 + gridSize * 2;
            g2d.drawLine(centerX - boltSize/2, centerY - boltSize, centerX + boltSize/2, centerY - boltSize/3);
            g2d.drawLine(centerX + boltSize/2, centerY - boltSize/3, centerX - boltSize/2, centerY + boltSize/3);
            g2d.drawLine(centerX - boltSize/2, centerY + boltSize/3, centerX + boltSize/2, centerY + boltSize);
            
            // Draw "x2" text
            g2d.setColor(new Color(255, 255, 255, (int)(150 * alpha)));
            g2d.setFont(new Font("Arial", Font.BOLD, 8 + gridSize * 2));
            g2d.drawString("x2", x + 4, y - 2);
            
            // Draw lifetime bar
            float lifePercent = lifetime / maxLifetime;
            int barWidth = (int)(totalSize * lifePercent);
            g2d.setColor(new Color(SPEED_TARGET_COLOR.getRed(), SPEED_TARGET_COLOR.getGreen(), SPEED_TARGET_COLOR.getBlue(), (int)(150 * alpha)));
            g2d.fillRect(x, y + totalSize + 2, barWidth, 3);
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
        speedTargets = new ArrayList<>();
        directionQueue = new LinkedList<>();
        soundEngine = new RetroSoundEngine();
        musicEngine = new MusicEngine();
        musicEngine.startMenuMusic();
        
        // Initialize stars for hyperspace effect
        stars = new ArrayList<>();
        for (int i = 0; i < NUM_STARS; i++) {
            stars.add(new Star());
        }
        
        // Load global high score from file
        globalHighScore = loadGlobalHighScore();
        sessionHighScore = 0;
        initGame();
        
        timer = new Timer(currentDelay, this);
        timer.start();
    }
    
    // Load global high score from file
    private int loadGlobalHighScore() {
        try {
            File file = new File(HIGH_SCORE_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                reader.close();
                if (line != null) {
                    return Integer.parseInt(line.trim());
                }
            }
        } catch (Exception e) {
            // If any error, just return 0
        }
        return 0;
    }
    
    // Save global high score to file
    private void saveGlobalHighScore() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(HIGH_SCORE_FILE));
            writer.println(globalHighScore);
            writer.close();
        } catch (Exception e) {
            // Silently ignore save errors
        }
    }
    
    // Check if player beat any records and announce
    private void checkAndAnnounceRecords() {
        // Check global record first (more important)
        if (score > globalHighScore && !globalRecordAnnounced) {
            globalHighScore = score;
            newGlobalRecord = true;
            globalRecordAnnounced = true;
            saveGlobalHighScore(); // Save immediately
            soundEngine.playGlobalRecord(); // Epic fanfare for global record
        }
        
        // Check session record (only if not already a global record)
        if (score > sessionHighScore && !sessionRecordAnnounced && !globalRecordAnnounced) {
            sessionHighScore = score;
            newSessionRecord = true;
            sessionRecordAnnounced = true;
            soundEngine.playSessionRecord(); // Short fanfare for session record
        } else if (score > sessionHighScore) {
            // Update session record silently if global was already announced
            sessionHighScore = score;
            newSessionRecord = true;
        }
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
        speedupTimer = 0;
        tempoLevel = 0;
        foodTimer = FOOD_TIME_LIMIT;
        currentDelay = NORMAL_DELAY;
        if (timer != null) {
            timer.setDelay(currentDelay);
        }
        if (musicEngine != null) {
            musicEngine.setTempoMultiplier(1.0);
        }
        
        gameOver = false;
        gameClose = false;
        gameStarted = false;
        newGlobalRecord = false;
        newSessionRecord = false;
        globalRecordAnnounced = false;
        sessionRecordAnnounced = false;
        spacePressed = false;
        autoFireCooldown = 0;
        particles.clear();
        bullets.clear();
        targets.clear();
        slowTargets.clear();
        shrinkTargets.clear();
        speedTargets.clear();
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
        
        // Reset food timer
        foodTimer = FOOD_TIME_LIMIT;
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
        int count = random.nextInt(12) + 1; // Double the targets (1-12 instead of 0-5)
        
        for (int i = 0; i < count; i++) {
            int attempts = 0;
            boolean validPosition = false;
            int tx = 0, ty = 0;
            
            // Random size: 60% 1x1, 30% 2x2, 10% 3x3
            int sizeRoll = random.nextInt(100);
            int gridSize;
            if (sizeRoll < 60) {
                gridSize = 1;
            } else if (sizeRoll < 90) {
                gridSize = 2;
            } else {
                gridSize = 3;
            }
            
            while (!validPosition && attempts < 50) {
                // Account for target size when generating position
                int maxX = (GAME_WIDTH - BLOCK_SIZE * (1 + gridSize)) / BLOCK_SIZE;
                int maxY = (GAME_HEIGHT - BLOCK_SIZE * (1 + gridSize)) / BLOCK_SIZE;
                tx = (random.nextInt(maxX) + 1) * BLOCK_SIZE;
                ty = (random.nextInt(maxY) + 1) * BLOCK_SIZE;
                validPosition = isValidTargetPosition(tx, ty, gridSize);
                attempts++;
            }
            
            if (validPosition) {
                // 12% slow target, 25% shrink target (more frequent!), 13% speed target, 50% dangerous target
                int roll = random.nextInt(100);
                if (roll < 12) {
                    slowTargets.add(new SlowTarget(tx, ty, gridSize));
                } else if (roll < 37) {
                    // 25% for shrink targets - much more frequent now!
                    shrinkTargets.add(new ShrinkTarget(tx, ty, gridSize));
                } else if (roll < 50) {
                    // 13% for speed targets
                    speedTargets.add(new SpeedTarget(tx, ty, gridSize));
                } else {
                    targets.add(new Target(tx, ty, getRandomTargetType(), gridSize));
                }
            }
        }
    }
    
    // Check if a target of given size at (tx, ty) would fit and not overlap
    private boolean isValidTargetPosition(int tx, int ty, int gridSize) {
        // Check all cells that the target would occupy
        for (int dx = 0; dx < gridSize; dx++) {
            for (int dy = 0; dy < gridSize; dy++) {
                int cellX = tx + dx * BLOCK_SIZE;
                int cellY = ty + dy * BLOCK_SIZE;
                
                // Check bounds
                if (cellX < BLOCK_SIZE || cellX >= GAME_WIDTH - BLOCK_SIZE ||
                    cellY < BLOCK_SIZE || cellY >= GAME_HEIGHT - BLOCK_SIZE) {
                    return false;
                }
                
                // Check food
                if (cellX == foodX && cellY == foodY) return false;
                
                // Check snake
                for (int[] segment : snakeList) {
                    if (segment[0] == cellX && segment[1] == cellY) return false;
                }
                
                // Check existing dangerous targets
                for (Target t : targets) {
                    if (t.occupiesCell(cellX, cellY)) return false;
                }
                
                // Check existing slow targets
                for (SlowTarget st : slowTargets) {
                    if (st.occupiesCell(cellX, cellY)) return false;
                }
                
                // Check existing shrink targets
                for (ShrinkTarget sht : shrinkTargets) {
                    if (sht.occupiesCell(cellX, cellY)) return false;
                }
                
                // Check existing speed targets
                for (SpeedTarget spt : speedTargets) {
                    if (spt.occupiesCell(cellX, cellY)) return false;
                }
            }
        }
        
        return true;
    }
    
    // Legacy method for backward compatibility
    private boolean isValidTargetPosition(int tx, int ty) {
        return isValidTargetPosition(tx, ty, 1);
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
        soundEngine.playShoot();
    }
    
    private void activateSlowdown() {
        slowdownTimer = SLOWDOWN_DURATION;
        speedupTimer = 0; // Cancel speedup if active
        currentDelay = SLOW_DELAY;
        timer.setDelay(currentDelay);
        soundEngine.playPowerUp();
    }
    
    private void activateSpeedup() {
        speedupTimer = SPEEDUP_DURATION;
        slowdownTimer = 0; // Cancel slowdown if active
        currentDelay = FAST_DELAY;
        timer.setDelay(currentDelay);
        soundEngine.playSpeedUp();
    }
    
    // Check if snake length crossed another 10-segment threshold and increase music tempo
    private void checkMusicTempoIncrease() {
        int newTempoLevel = snakeLength / 10; // Every 10 segments = new tempo level
        if (newTempoLevel > tempoLevel) {
            tempoLevel = newTempoLevel;
            double newTempo = 1.0 + tempoLevel * 0.2; // Increase by 20% for each 10 segments
            musicEngine.setTempoMultiplier(newTempo);
        }
    }
    
    // LINE-BASED collision: bullet must be on the same line as the target (not adjacent lines)
    // Returns true if bullet hit and was removed
    private boolean checkBulletCollisions(Bullet b, int bulletIndex) {
        // Check collision with dangerous targets (square)
        for (int j = targets.size() - 1; j >= 0; j--) {
            Target t = targets.get(j);
            if (checkLineCollision(b, t.x, t.y, t.getPixelSize())) {
                int centerX = t.x + t.getPixelSize() / 2;
                int centerY = t.y + t.getPixelSize() / 2;
                spawnParticles(centerX, centerY, 20 * t.gridSize, t.type.color);
                soundEngine.playExplosion();
                score += t.type.points * t.gridSize; // More points for bigger targets
                snakeLength += t.type.points * t.gridSize;
                checkMusicTempoIncrease();
                checkAndAnnounceRecords();
                targetsHit++;
                targets.remove(j);
                bullets.remove(bulletIndex);
                return true;
            }
        }
        
        // Check collision with slow targets (circle)
        for (int j = slowTargets.size() - 1; j >= 0; j--) {
            SlowTarget st = slowTargets.get(j);
            if (checkLineCollision(b, st.x, st.y, st.getPixelSize())) {
                int centerX = st.x + st.getPixelSize() / 2;
                int centerY = st.y + st.getPixelSize() / 2;
                spawnParticles(centerX, centerY, 20 * st.gridSize, SLOW_TARGET_COLOR);
                soundEngine.playExplosion();
                activateSlowdown();
                targetsHit++;
                slowTargets.remove(j);
                bullets.remove(bulletIndex);
                return true;
            }
        }
        
        // Check collision with shrink targets (triangle)
        for (int j = shrinkTargets.size() - 1; j >= 0; j--) {
            ShrinkTarget sht = shrinkTargets.get(j);
            if (checkLineCollision(b, sht.x, sht.y, sht.getPixelSize())) {
                int centerX = sht.x + sht.getPixelSize() / 2;
                int centerY = sht.y + sht.getPixelSize() / 2;
                spawnParticles(centerX, centerY, 20 * sht.gridSize, SHRINK_TARGET_COLOR);
                soundEngine.playExplosion();
                soundEngine.playShrink();
                snakeLength = Math.max(1, snakeLength / 2);
                while (snakeList.size() > snakeLength) {
                    snakeList.remove(0);
                }
                targetsHit++;
                shrinkTargets.remove(j);
                bullets.remove(bulletIndex);
                return true;
            }
        }
        
        // Check collision with speed targets (diamond)
        for (int j = speedTargets.size() - 1; j >= 0; j--) {
            SpeedTarget spt = speedTargets.get(j);
            if (checkLineCollision(b, spt.x, spt.y, spt.getPixelSize())) {
                int centerX = spt.x + spt.getPixelSize() / 2;
                int centerY = spt.y + spt.getPixelSize() / 2;
                spawnParticles(centerX, centerY, 20 * spt.gridSize, SPEED_TARGET_COLOR);
                soundEngine.playExplosion();
                activateSpeedup();
                targetsHit++;
                speedTargets.remove(j);
                bullets.remove(bulletIndex);
                return true;
            }
        }
        
        return false;
    }
    
    // Check if bullet is on the same line and inside target bounds
    // Uses line-based collision - bullet must actually hit the target (same row/column)
    private boolean checkLineCollision(Bullet b, int targetX, int targetY, int targetSize) {
        // Target bounds (with small tolerance for smoother hits)
        float tolerance = 4; // Small tolerance for precise but not frustrating hits
        float tLeft = targetX - tolerance;
        float tRight = targetX + targetSize + tolerance;
        float tTop = targetY - tolerance;
        float tBottom = targetY + targetSize + tolerance;
        
        // Bullet is moving horizontally (left or right)
        if (b.vy == 0 && b.vx != 0) {
            // Check if bullet Y is within target's Y range (same horizontal line)
            if (b.y >= tTop && b.y <= tBottom) {
                // Check if bullet X is inside target's X range
                if (b.x >= tLeft && b.x <= tRight) {
                    return true;
                }
            }
        }
        // Bullet is moving vertically (up or down)
        else if (b.vx == 0 && b.vy != 0) {
            // Check if bullet X is within target's X range (same vertical line)
            if (b.x >= tLeft && b.x <= tRight) {
                // Check if bullet Y is inside target's Y range
                if (b.y >= tTop && b.y <= tBottom) {
                    return true;
                }
            }
        }
        
        return false;
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
            drawSpeedTargets(g2d);
            drawFood(g2d);
            drawSnake(g2d);
            drawBullets(g2d);
            drawParticles(g2d);
            
            // Draw slowdown indicator
            if (slowdownTimer > 0) {
                drawSlowdownIndicator(g2d);
            }
            
            // Draw speedup indicator
            if (speedupTimer > 0) {
                drawSpeedupIndicator(g2d);
            }
            
            // Always draw food timer during game
            drawFoodTimer(g2d);
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
        
        // Border color changes when slowed or sped up
        Color borderColor;
        if (speedupTimer > 0) {
            borderColor = SPEED_TARGET_COLOR;
        } else if (slowdownTimer > 0) {
            borderColor = SLOW_TARGET_COLOR;
        } else {
            borderColor = new Color(0, 255, 150);
        }
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
        boolean isFast = speedupTimer > 0;
        
        for (int i = 0; i < size; i++) {
            int[] segment = snakeList.get(i);
            float progress = (float) i / Math.max(size - 1, 1);
            
            Color segmentColor;
            if (i == size - 1) {
                if (isFast) {
                    segmentColor = SNAKE_FAST_COLOR;
                    g2d.setColor(new Color(255, 180, 50, 80));
                } else if (isSlowed) {
                    segmentColor = SNAKE_SLOW_COLOR;
                    g2d.setColor(new Color(100, 150, 255, 80));
                } else {
                    segmentColor = SNAKE_HEAD_COLOR;
                    g2d.setColor(SNAKE_GLOW);
                }
                g2d.fill(new Ellipse2D.Float(segment[0] - 4, segment[1] - 4, BLOCK_SIZE + 8, BLOCK_SIZE + 8));
            } else {
                if (isFast) {
                    // Orange/yellow gradient when fast
                    int r = (int)(255 + (200 - 255) * (1 - progress));
                    int gr = (int)(180 + (100 - 180) * (1 - progress));
                    int b = (int)(50 + (0 - 50) * (1 - progress));
                    segmentColor = new Color(r, gr, b);
                } else if (isSlowed) {
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
    
    private void drawSpeedTargets(Graphics2D g2d) {
        for (SpeedTarget spt : speedTargets) {
            spt.draw(g2d);
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
    
    private void drawSpeedupIndicator(Graphics2D g2d) {
        // Draw speedup timer bar at top
        float progress = (float) speedupTimer / SPEEDUP_DURATION;
        int barWidth = (int)(GAME_WIDTH * 0.6f);
        int barHeight = 8;
        int barX = (GAME_WIDTH - barWidth) / 2;
        int barY = 15;
        
        // Background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(barX - 5, barY - 5, barWidth + 10, barHeight + 20, 10, 10);
        
        // Bar background
        g2d.setColor(new Color(100, 50, 0));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);
        
        // Bar fill (orange/yellow gradient effect)
        g2d.setColor(SPEED_TARGET_COLOR);
        g2d.fillRoundRect(barX, barY, (int)(barWidth * progress), barHeight, 4, 4);
        
        // Text
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        String text = "SPEED x2 - " + String.format("%.1f", speedupTimer / 24.0f) + "s";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, barX + (barWidth - fm.stringWidth(text)) / 2, barY + barHeight + 14);
    }
    
    private void drawFoodTimer(Graphics2D g2d) {
        // Draw food timer near the food
        float timeLeft = foodTimer / 12.0f; // Convert to seconds
        float progress = (float) foodTimer / FOOD_TIME_LIMIT;
        
        // Color changes from green to yellow to red based on time left
        Color timerColor;
        if (progress > 0.5f) {
            timerColor = new Color(100, 255, 100); // Green
        } else if (progress > 0.25f) {
            timerColor = new Color(255, 255, 0); // Yellow
        } else {
            // Blink red when critical
            int blink = (foodTimer % 12 < 6) ? 255 : 150;
            timerColor = new Color(blink, 50, 50); // Red (blinking)
        }
        
        // Draw circular timer around food
        int timerRadius = BLOCK_SIZE + 8;
        int centerX = foodX + BLOCK_SIZE / 2;
        int centerY = foodY + BLOCK_SIZE / 2;
        
        // Background arc
        g2d.setColor(new Color(50, 50, 50, 150));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawOval(centerX - timerRadius, centerY - timerRadius, timerRadius * 2, timerRadius * 2);
        
        // Progress arc
        g2d.setColor(timerColor);
        g2d.setStroke(new BasicStroke(4));
        int arcAngle = (int)(360 * progress);
        g2d.drawArc(centerX - timerRadius, centerY - timerRadius, timerRadius * 2, timerRadius * 2, 90, -arcAngle);
        
        // Timer text
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        String timerText = String.format("%.1f", timeLeft);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setColor(timerColor);
        g2d.drawString(timerText, centerX - fm.stringWidth(timerText) / 2, centerY - timerRadius - 5);
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
        
        // Global high score (gold color)
        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Global Best: " + globalHighScore, x, y);
        y += 16;
        
        // Session high score (silver color)
        g2d.setColor(new Color(180, 180, 200));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Session Best: " + sessionHighScore, x, y);
        y += lineHeight;
        
        // Divider
        g2d.setColor(new Color(100, 100, 150));
        g2d.drawLine(x, y - 5, GAME_WIDTH + STATS_WIDTH - 15, y - 5);
        y += 12; // Added more spacing after divider
        
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
        y += 12; // Added more spacing after divider
        
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
        y += 12; // Added more spacing after divider
        
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
        y += 18;
        
        // Speed target (diamond)
        g2d.setColor(SPEED_TARGET_COLOR);
        int[] dxPoints = {x + 5, x + 10, x + 5, x};
        int[] dyPoints = {y - 9, y - 4, y + 1, y - 4};
        g2d.fillPolygon(dxPoints, dyPoints, 4);
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawString("Speed (x2)", x + 14, y);
        y += lineHeight + 5;
        
        // Legend explanation
        g2d.setColor(new Color(120, 120, 140));
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        g2d.drawString("Square = Dangerous", x, y);
        y += 14;
        g2d.drawString("Other shapes = Safe", x, y);
        y += lineHeight;
        
        // Controls hint
        g2d.setColor(new Color(100, 100, 120));
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        g2d.drawString("ESC - End Game", x, y);
        y += 14;
        g2d.drawString("Arrows - Move", x, y);
        y += 14;
        g2d.drawString("Space - Shoot", x, y);
        y += 14;
        // Show mute status
        if (soundMuted) {
            g2d.setColor(new Color(255, 100, 100));
            g2d.drawString("Backspace - Sound OFF", x, y);
        } else {
            g2d.setColor(new Color(100, 255, 100));
            g2d.drawString("Backspace - Sound ON", x, y);
        }
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
        g2d.drawString(controls, (GAME_WIDTH - fm.stringWidth(controls)) / 2, GAME_HEIGHT - 100);
        
        // Sound control hint
        if (soundMuted) {
            g2d.setColor(new Color(255, 100, 100));
            String soundHint = "Backspace - Sound OFF (press to enable)";
            fm = g2d.getFontMetrics();
            g2d.drawString(soundHint, (GAME_WIDTH - fm.stringWidth(soundHint)) / 2, GAME_HEIGHT - 75);
        } else {
            g2d.setColor(new Color(100, 255, 100));
            String soundHint = "Backspace - Mute Sound";
            fm = g2d.getFontMetrics();
            g2d.drawString(soundHint, (GAME_WIDTH - fm.stringWidth(soundHint)) / 2, GAME_HEIGHT - 75);
        }
        
        g2d.setColor(new Color(255, 100, 100, 200));
        String warning = "Don't collide with SQUARE targets!";
        fm = g2d.getFontMetrics();
        g2d.drawString(warning, (GAME_WIDTH - fm.stringWidth(warning)) / 2, GAME_HEIGHT - 45);
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
        
        int recordY = GAME_HEIGHT / 2 + 35;
        
        if (newGlobalRecord) {
            // Flashing gold text for GLOBAL record
            int flashAlpha = (int)(Math.abs(Math.sin(foodPulse * 3)) * 155 + 100);
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            g2d.setColor(new Color(255, 215, 0, flashAlpha));
            String newRecord = "NEW GLOBAL RECORD!";
            fm = g2d.getFontMetrics();
            g2d.drawString(newRecord, (GAME_WIDTH - fm.stringWidth(newRecord)) / 2, recordY);
            
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            g2d.setColor(new Color(255, 255, 200));
            String congrats = "You beat the all-time record!";
            fm = g2d.getFontMetrics();
            g2d.drawString(congrats, (GAME_WIDTH - fm.stringWidth(congrats)) / 2, recordY + 25);
            recordY += 55;
        } else if (newSessionRecord) {
            // Silver text for session record
            int flashAlpha = (int)(Math.abs(Math.sin(foodPulse * 2)) * 100 + 155);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.setColor(new Color(200, 200, 255, flashAlpha));
            String newRecord = "NEW SESSION RECORD!";
            fm = g2d.getFontMetrics();
            g2d.drawString(newRecord, (GAME_WIDTH - fm.stringWidth(newRecord)) / 2, recordY);
            
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            g2d.setColor(new Color(200, 200, 220));
            String congrats = "Best score this session!";
            fm = g2d.getFontMetrics();
            g2d.drawString(congrats, (GAME_WIDTH - fm.stringWidth(congrats)) / 2, recordY + 22);
            recordY += 50;
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
                if (slowdownTimer == 0 && speedupTimer == 0) {
                    currentDelay = NORMAL_DELAY;
                    timer.setDelay(currentDelay);
                }
            }
            
            // Update speedup timer
            if (speedupTimer > 0) {
                speedupTimer--;
                if (speedupTimer == 0 && slowdownTimer == 0) {
                    currentDelay = NORMAL_DELAY;
                    timer.setDelay(currentDelay);
                }
            }
            
            // Update food timer - game over if time runs out
            if (foodTimer > 0) {
                foodTimer--;
                if (foodTimer == 0) {
                    spawnParticles(foodX, foodY, 25, FOOD_COLOR);
                    soundEngine.playExplosion();
                    endGame();
                    return;
                }
            }
            
            // Auto-fire when holding space
            if (spacePressed && autoFireCooldown > 0) {
                autoFireCooldown--;
            }
            if (spacePressed && autoFireCooldown == 0) {
                shoot();
                autoFireCooldown = AUTO_FIRE_DELAY;
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
            
            speedTargets.removeIf(SpeedTarget::isDead);
            for (SpeedTarget spt : speedTargets) {
                spt.update();
            }
            
            // Update bullets - check collision BEFORE and AFTER moving to fix close-range bug
            for (int i = bullets.size() - 1; i >= 0; i--) {
                Bullet b = bullets.get(i);
                
                // Check collision BEFORE moving (point-blank shots)
                if (checkBulletCollisions(b, i)) continue;
                
                b.update();
                
                if (b.isOutOfBounds()) {
                    bullets.remove(i);
                    continue;
                }
                
                // Check collision with food - GAME OVER! (line-based)
                if (checkLineCollision(b, foodX, foodY, BLOCK_SIZE)) {
                    spawnParticles(foodX, foodY, 25, FOOD_COLOR);
                    soundEngine.playExplosion();
                    endGame();
                    break;
                }
                
                // Check collision AFTER moving (uses line-based detection)
                checkBulletCollisions(b, i);
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
                spawnParticles(Math.max(0, Math.min(x1, GAME_WIDTH - BLOCK_SIZE)), 
                              Math.max(0, Math.min(y1, GAME_HEIGHT - BLOCK_SIZE)), 20, SNAKE_HEAD_COLOR);
                soundEngine.playHit();
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
                    soundEngine.playEat();
                    spawnFood();
                    snakeLength++;
                    score++;
                    foodEaten++;
                    checkMusicTempoIncrease();
                    checkAndAnnounceRecords();
                }
                
                // Check dangerous target collision (square) - GAME OVER!
                // Only active targets are dangerous (after 3 second spawn delay)
                // Check all cells occupied by target (for multi-cell targets)
                for (Target t : targets) {
                    if (t.isActive() && t.occupiesCell(x1, y1)) {
                        int centerX = t.x + t.getPixelSize() / 2;
                        int centerY = t.y + t.getPixelSize() / 2;
                        spawnParticles(centerX, centerY, 25 * t.gridSize, t.type.color);
                        soundEngine.playExplosion();
                        endGame();
                        break;
                    }
                }
                
                // Slow targets are safe to pass through (no collision damage)
                
                // Check self collision
                for (int i = 0; i < snakeList.size() - 1; i++) {
                    int[] segment = snakeList.get(i);
                    if (segment[0] == x1 && segment[1] == y1) {
                        spawnParticles(x1, y1, 20, SNAKE_HEAD_COLOR);
                        soundEngine.playHit();
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
        
        // Final record check
        if (score > sessionHighScore) {
            sessionHighScore = score;
            newSessionRecord = true;
        }
        if (score > globalHighScore) {
            globalHighScore = score;
            newGlobalRecord = true;
            saveGlobalHighScore();
        }
        
        // Play appropriate sound
        if (newGlobalRecord) {
            // Don't play again if already announced during game
            if (!globalRecordAnnounced) {
                soundEngine.playGlobalRecord();
            }
        } else {
            soundEngine.playGameOver();
        }
        
        musicEngine.startMenuMusic();
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
        
        // Backspace toggles mute for all sounds and music
        if (key == KeyEvent.VK_BACK_SPACE) {
            soundMuted = !soundMuted;
            return;
        }
        
        if (key == KeyEvent.VK_ESCAPE) {
            if (!gameStarted || gameClose) {
                musicEngine.stopMusic();
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
                musicEngine.startGameMusic(); // Switch to game music
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
                musicEngine.startGameMusic(); // Switch to game music when starting
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
                spacePressed = true;
                shoot();
                autoFireCooldown = AUTO_FIRE_DELAY;
            }
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            spacePressed = false;
        }
    }
    
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
