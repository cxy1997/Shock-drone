package cxy.twitch;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class Joystick extends AppCompatImageView{

    public Joystick(Context context) {
        super(context);
    }

    public Joystick(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Joystick(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void move(float x, float y) {
        if (x * x + y * y > 210 * 210) {
            double angle = Math.atan2(y, x);
            x = (float) Math.cos(angle) * 210;
            y = (float) Math.sin(angle) * 210;
        }
        setFrame((int) (360 + x - getWidth() / 2), (int) (660 + y - getHeight() / 2), (int) (360 + x + getWidth() / 2), (int) (660 + y + getHeight() / 2));
    }
}
