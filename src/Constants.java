import java.awt.Color;

public class Constants {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final int SNAKE_SIZE = 20;
    public static final int SNAKE_LENGTH = 20;
    public static final int FIXED_FPS = 90;
    public static final double MIN_SPEED_STEP = 0.5;
    public static final double MAX_SPEED_STEP = 4.0;
    public static final double SPEED_STEP_DELTA = 0.2;
    public static final long MESSAGE_DURATION_MS = 1500;

    public static final Color BACKGROUND_COLOR = new Color(10, 10, 25);
    public static final Color SNAKE_COLOR = new Color(50, 205, 50);
    public static final Color BORDER_COLOR = new Color(70, 130, 180, 150);
    public static final Color INFO_COLOR = new Color(220, 220, 255);
    public static final Color GRID_COLOR = new Color(40, 40, 60, 100);
    public static final Color PAUSE_OVERLAY = new Color(0, 0, 0, 180);

    public static final int KEY_1 = 49;
    public static final int KEY_2 = 50;
    public static final int KEY_SPACE = 32;
    public static final int KEY_ESCAPE = 27;

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}