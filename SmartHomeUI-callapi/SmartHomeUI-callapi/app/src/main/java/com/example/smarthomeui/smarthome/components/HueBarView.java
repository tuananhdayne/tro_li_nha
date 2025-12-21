package com.example.smarthomeui.smarthome.components;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class HueBarView extends View {

    public interface OnHueChange { void onChange(float hue); }

    private float hue = 200f; // 0..360
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumb = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader shader;
    private OnHueChange listener;

    public HueBarView(Context c, @Nullable AttributeSet a){ super(c,a);
        thumb.setStyle(Paint.Style.STROKE);
        thumb.setStrokeWidth(4f);
        thumb.setColor(Color.WHITE);
    }

    public void setOnHueChange(OnHueChange l){ this.listener = l; }
    public void setHue(float h){ this.hue = h; invalidate(); }
    public float getHue(){ return hue; }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        int[] colors = new int[]{
                Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
        };
        shader = new LinearGradient(0,0,0,h, colors, null, Shader.TileMode.CLAMP);
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        paint.setShader(shader);
        c.drawRoundRect(0,0,getWidth(),getHeight(),8,8, paint);
        paint.setShader(null);

        float y = (hue / 360f) * getHeight();
        c.drawLine(0, y, getWidth(), y, thumb);
        thumb.setColor(0x80000000); c.drawLine(0, y+2, getWidth(), y+2, thumb);
        thumb.setColor(Color.WHITE);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        float y = Math.max(0f, Math.min(getHeight(), e.getY()));
        hue = (y / Math.max(1f, getHeight())) * 360f;
        if (listener != null) listener.onChange(hue);
        invalidate();
        return true;
    }
}
