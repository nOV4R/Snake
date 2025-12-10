import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class Snake extends JPanel implements KeyListener, Runnable {
    private ArrayList<SnakeSegment> snake;
    private double directionX;
    private double directionY;
    private volatile boolean isPaused;
    private volatile boolean isRunning;
    private Random random;
    private volatile double speedStep;
    private Thread gameThread;

    private double accumulatedX = 0;
    private double accumulatedY = 0;
    private volatile String speedMessage = "";
    private long speedMessageTime = 0;

    public Snake() {
        initializeGame();
    }

    private void initializeGame() {
        try {
            setPreferredSize(new Dimension(Constants.WIDTH, Constants.HEIGHT));
            setBackground(Constants.BACKGROUND_COLOR);
            setFocusable(true);
            addKeyListener(this);

            random = new Random();
            speedStep = 2.0;
            isPaused = false;
            isRunning = true;

            initializeSnake();
        } catch (Exception e) {
            handleInitializationError(e);
        }
    }

    private void handleInitializationError(Exception e) {
        System.err.println("Ошибка инициализации: " + e.getMessage());
        e.printStackTrace();

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                    "Не удалось инициализировать игру:\n" + e.getMessage(),
                    "Ошибка инициализации",
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    private void initializeSnake() {
        snake = new ArrayList<>();

        int startX = Constants.SNAKE_SIZE + random.nextInt(
                Constants.WIDTH - 2 * Constants.SNAKE_SIZE - Constants.SNAKE_SIZE * Constants.SNAKE_LENGTH);
        int startY = Constants.SNAKE_SIZE + random.nextInt(
                Constants.HEIGHT - 2 * Constants.SNAKE_SIZE - Constants.SNAKE_SIZE * Constants.SNAKE_LENGTH);

        setRandomDirection();

        for (int i = 0; i < Constants.SNAKE_LENGTH; i++) {
            double x = startX - i * directionX * Constants.SNAKE_SIZE;
            double y = startY - i * directionY * Constants.SNAKE_SIZE;

            x = Constants.clamp(x, 0, Constants.WIDTH - Constants.SNAKE_SIZE);
            y = Constants.clamp(y, 0, Constants.HEIGHT - Constants.SNAKE_SIZE);

            snake.add(new SnakeSegment(x, y));
        }
    }

    private void setRandomDirection() {
        try {
            double angle = random.nextDouble() * 2 * Math.PI;
            directionX = Math.cos(angle);
            directionY = Math.sin(angle);
        } catch (Exception e) {
            directionX = 1;
            directionY = 0;
            System.err.println("Ошибка при установке направления: " + e.getMessage());
        }
    }

    private void updateSnakePosition() {
        if (snake.isEmpty()) return;

        SnakeSegment head = snake.get(0);
        updateAccumulatedMovement();

        double newX = head.getX() + accumulatedX;
        double newY = head.getY() + accumulatedY;

        applyIntegerMovement();
        handleBorderCollisions(newX, newY);

        snake.add(0, new SnakeSegment(newX, newY));
        maintainSnakeLength();
    }

    private void updateAccumulatedMovement() {
        accumulatedX += directionX * speedStep;
        accumulatedY += directionY * speedStep;
    }

    private void applyIntegerMovement() {
        int moveX = (int) Math.round(accumulatedX);
        int moveY = (int) Math.round(accumulatedY);
        accumulatedX -= moveX;
        accumulatedY -= moveY;
    }


    private void handleBorderCollisions(double newX, double newY) {
        boolean hitBorder = false;

// Проверка левой границы
        if (newX < 0) {
            directionX = Math.abs(directionX); // Отражаем вправо
            newX = 0;
            accumulatedX = 0;
            hitBorder = true;
        }
        // Проверка правой границы
        else if (newX >= Constants.WIDTH - Constants.SNAKE_SIZE) {
            directionX = -Math.abs(directionX); // Отражаем влево
            newX = Constants.WIDTH - Constants.SNAKE_SIZE;
            accumulatedX = 0;
            hitBorder = true;
        }

        // Проверка верхней границы
        if (newY < 0) {
            directionY = Math.abs(directionY); // Отражаем вниз
            newY = 0;
            accumulatedY = 0;
            hitBorder = true;
        }
        // Проверка нижней границы
        else if (newY >= Constants.HEIGHT - Constants.SNAKE_SIZE) {
            directionY = -Math.abs(directionY); // Отражаем вверх
            newY = Constants.HEIGHT - Constants.SNAKE_SIZE;
            accumulatedY = 0;
            hitBorder = true;
        }

        // Нормализация направления
        if (hitBorder) {
            normalizeDirection();
        }
    }

    private void normalizeDirection() {
        double length = Math.sqrt(directionX * directionX + directionY * directionY);
        if (length > 0) {
            directionX /= length;
            directionY /= length;
        } else {
            directionX = 1;
            directionY = 0;
        }
    }

    private void maintainSnakeLength() {
        if (snake.size() > Constants.SNAKE_LENGTH) {
            snake.remove(snake.size() - 1);
        }
    }

    private synchronized void changeSpeed(boolean increase) {
        try {
            if (increase) {
                speedStep = Math.min(Constants.MAX_SPEED_STEP, speedStep + Constants.SPEED_STEP_DELTA);
                speedMessage = "Скорость ↑";
            } else {
                speedStep = Math.max(Constants.MIN_SPEED_STEP, speedStep - Constants.SPEED_STEP_DELTA);
                speedMessage = "Скорость ↓";
            }
            speedMessageTime = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println("Ошибка при изменении скорости: " + e.getMessage());
        }
    }

    public void startGame() {
        if (gameThread == null) {
            gameThread = new Thread(this, "Snake-Game-Thread");
            gameThread.setDaemon(true);
            gameThread.start();
        }
    }

    public void stopGame() {
        isRunning = false;
        if (gameThread != null) {
            try {
                gameThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Поток игры был прерван: " + e.getMessage());
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        try {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            drawGrid(g2d);
            drawBorders(g2d);
            drawSnake(g2d);
            drawInfo(g2d);
        } catch (Exception e) {
            handleDrawingError(g, e);
        }
    }

    private void handleDrawingError(Graphics g, Exception e) {
        System.err.println("Ошибка отрисовки: " + e.getMessage());
        g.setColor(Color.RED);
        g.drawString("Ошибка отрисовки", 50, 50);
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(Constants.GRID_COLOR);
        g2d.setStroke(new BasicStroke(1));

        for (int x = 0; x < Constants.WIDTH; x += 40) {
            g2d.drawLine(x, 0, x, Constants.HEIGHT);
        }

        for (int y = 0; y < Constants.HEIGHT; y += 40) {
            g2d.drawLine(0, y, Constants.WIDTH, y);
        }
    }

    private void drawBorders(Graphics2D g2d) {
        g2d.setColor(Constants.BORDER_COLOR);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(1, 1, Constants.WIDTH - 3, Constants.HEIGHT - 3);
    }
    private void drawSnake(Graphics2D g2d) {
        if (snake == null || snake.isEmpty()) return;

        for (int i = 0; i < snake.size(); i++) {
            SnakeSegment segment = snake.get(i);
            float intensity = 1.0f - (i * 0.05f);
            intensity = Math.max(0.3f, intensity);

            int r = (int) (Constants.SNAKE_COLOR.getRed() * intensity);
            int g = (int) (Constants.SNAKE_COLOR.getGreen() * intensity);
            int b = (int) (Constants.SNAKE_COLOR.getBlue() * intensity);

            Color segmentColor = new Color(r, g, b);
            g2d.setColor(segmentColor);
            g2d.fillOval(segment.getIntX(), segment.getIntY(),
                    Constants.SNAKE_SIZE, Constants.SNAKE_SIZE);

            g2d.setColor(segmentColor.darker());
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(segment.getIntX(), segment.getIntY(),
                    Constants.SNAKE_SIZE, Constants.SNAKE_SIZE);
        }
    }

    private void drawInfo(Graphics2D g2d) {
        g2d.setColor(Constants.INFO_COLOR);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 16));

        String speedInfo = String.format("Скорость: %.2f пикс/кадр", speedStep);
        g2d.drawString(speedInfo, 20, 30);

        double angleDeg = Math.atan2(directionY, directionX) * 180 / Math.PI;
        String directionInfo = String.format("Направление: [%.2f, %.2f] ∠%.1f°",
                directionX, directionY, angleDeg);
        g2d.drawString(directionInfo, 20, 60);

        if (!snake.isEmpty()) {
            SnakeSegment head = snake.get(0);
            String positionInfo = String.format("Позиция: [%3d, %3d]",
                    head.getIntX(), head.getIntY());
            g2d.drawString(positionInfo, 20, 90);
        }

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2d.drawString("УПРАВЛЕНИЕ:", 20, Constants.HEIGHT - 80);
        g2d.drawString("1    - Увеличить скорость", 40, Constants.HEIGHT - 60);
        g2d.drawString("2    - Уменьшить скорость", 40, Constants.HEIGHT - 40);
        g2d.drawString("ПРОБЕЛ - Пауза/Продолжить", 40, Constants.HEIGHT - 20);
        g2d.drawString("ESC  - Выход", 40, Constants.HEIGHT);

        if (!speedMessage.isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - speedMessageTime < Constants.MESSAGE_DURATION_MS) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
                g2d.drawString(speedMessage, Constants.WIDTH - 150, Constants.HEIGHT - 100);
            } else {
                speedMessage = "";
            }
        }

        if (isPaused) {
            g2d.setColor(Constants.PAUSE_OVERLAY);
            g2d.fillRect(Constants.WIDTH / 2 - 120, Constants.HEIGHT / 2 - 50, 240, 100);

            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.drawString("ПАУЗА", Constants.WIDTH / 2 - 70, Constants.HEIGHT / 2 + 10);
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerFrame = 1_000_000_000.0 / Constants.FIXED_FPS;
        double delta = 0;

        while (isRunning) {
            try {
                long now = System.nanoTime();
                delta += (now - lastTime) / nsPerFrame;
                lastTime = now;

                while (delta >= 1) {
                    if (!isPaused) {
                        updateSnakePosition();
                    }
                    delta--;
                    SwingUtilities.invokeLater(this::repaint);
                }
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Игровой цикл прерван: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("Ошибка в игровом цикле: " + e.getMessage());
                e.printStackTrace();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            int keyCode = e.getKeyCode();

            if (keyCode == Constants.KEY_1) {
                changeSpeed(true);
            } else if (keyCode == Constants.KEY_2) {
                changeSpeed(false);
            } else if (keyCode == Constants.KEY_SPACE) {
                isPaused = !isPaused;
            } else if (keyCode == Constants.KEY_ESCAPE) {
                stopGame();
                SwingUtilities.getWindowAncestor(this).dispose();
                System.exit(0);
            }
        } catch (Exception ex) {
            System.err.println("Ошибка обработки клавиши: " + ex.getMessage());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        try {
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Змейка с отражением от границ");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                Snake game = new Snake();
                frame.getContentPane().add(game);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setResizable(false);
                frame.setVisible(true);

                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        game.stopGame();
                    }
                });

                game.requestFocusInWindow();
                game.startGame();
            });
        } catch (Exception e) {
            System.err.println("Фатальная ошибка при запуске: " + e.getMessage());
            e.printStackTrace();

            JOptionPane.showMessageDialog(null,
                    "Не удалось запустить игру:\n" + e.getMessage(),
                    "Критическая ошибка",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}