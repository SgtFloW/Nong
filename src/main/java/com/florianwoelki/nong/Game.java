package com.florianwoelki.nong;

import com.florianwoelki.nong.entity.Ball;
import com.florianwoelki.nong.entity.Player;
import com.florianwoelki.nong.input.Keyboard;
import com.florianwoelki.nong.neuralnetwork.NeuralNetwork;
import com.florianwoelki.nong.util.FileManager;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.image.BufferStrategy;

/**
 * Created by Florian Woelki on 15.12.16.
 * <p>
 * <summary>
 * This class represents the basic main game.
 * </summary>
 */
public class Game extends Canvas implements Runnable {

    private Window window;
    @Getter
    private Keyboard keyboard;

    private Thread thread;
    private boolean running;

    private Player player;
    @Getter
    private Ball ball;

    @Setter
    @Getter
    private int score;

    /**
     * Constructor of the game.
     * Initialize everything.
     */
    public Game() {
        this.keyboard = new Keyboard();

        this.requestFocus();
        this.setFocusable(true);
        this.addKeyListener(keyboard);

        this.window = new Window(this);
        this.window.setVisible(true);

        this.player = new Player(this);
        this.ball = new Ball(this, getWidth() / 2, getHeight() / 2);
        this.ball.setSpeed((float) Math.toRadians(-45));
    }

    /**
     * Start the game thread.
     */
    public synchronized void start() {
        if(running) {
            return;
        }

        if(FileManager.exist("brain")) {
            NeuralNetwork neuralNetwork = FileManager.load("brain");
            player.setBrain(neuralNetwork);
        }

        running = true;
        thread = new Thread(this, "Game Window");
        thread.start();
    }

    /**
     * Stop the game thread.
     */
    public synchronized void stop() {
        if(!running) {
            return;
        }

        running = false;
        try {
            thread.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run method of the thread.
     */
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double delta = 0;
        double ns = 1000000000d / 60d;
        long lastTimer = System.currentTimeMillis();

        int fps = 0, ups = 0;

        // Game Loop
        while(running) {
            boolean shouldRender = false;
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while(delta >= 1) {
                delta--;
                update();
                ups++;
                shouldRender = true;
            }

            try {
                Thread.sleep(3);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }

            if(shouldRender) {
                render();
                fps++;
            }

            if(System.currentTimeMillis() - lastTimer > 1000) {
                lastTimer += 1000;
                System.out.println("FPS: " + fps + ", UPS: " + ups);
                ups = fps = 0;
                FileManager.save(player.getBrain(), "brain"); // performance...
            }
        }

        stop();
    }

    /**
     * This method updates the game with all components.
     */
    private void update() {
        keyboard.update();
        player.update();
        ball.update();
        ball.checkCollision(player);
    }

    /**
     * This method renders the game with all components.
     */
    private void render() {
        BufferStrategy bs = getBufferStrategy();
        if(bs == null) {
            createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        player.render(g);
        ball.render(g);
        g.drawString("" + score, getWidth() / 2, 80);
        player.getBrain().render(g, new Rectangle(150, 0, 200, 250));
        g.dispose();
        bs.show();
    }

}
