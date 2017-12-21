package cxy.twitch;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.SurfaceView;

import java.util.LinkedList;

public class Touchpad extends SurfaceView {
    private Thread drawThread;
    private Path mpath;
    private LinkedList<Pair<Float, Float>> pointList;
    private Pair<Float, Float> prev;
    private Paint normalPaint, unfinishedPaint, finishedPaint;
    private Double advanceSpeed = 0.2, turningSpeed = 0.2865101472;

    public Touchpad(Context context) {
        super(context);
        init();
    }

    public Touchpad(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Touchpad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        setFocusable(true);
        setFocusableInTouchMode(true);
        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }
}