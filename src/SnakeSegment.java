public class SnakeSegment {
    private double x;
    private double y;

    public SnakeSegment(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public int getIntX() { return (int) Math.round(x); }
    public int getIntY() { return (int) Math.round(y); }
    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void move(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }
}