package de.tum.cit.fop.maze;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
public abstract class GameObject {
    protected float x, y;
    protected float width, height;
    protected Rectangle bounds;
    public GameObject(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new Rectangle(x, y, width, height);
    }

    public abstract void render(SpriteBatch batch);
    public Rectangle getBounds() {
        return bounds;
    }
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.bounds.setPosition(x, y);
    }
    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }
    public float getWidth() {
        return width;
    }
    public float getHeight() {
        return height;
    }
}
