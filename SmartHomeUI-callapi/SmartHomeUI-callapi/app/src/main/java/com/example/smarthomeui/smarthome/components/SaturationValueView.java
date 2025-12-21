package com.example.smarthomeui.smarthome.components;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class SaturationValueView extends View {

    public interface OnSVChange { void onChange(float s, float v); }

    private float hue = 200f;   // 0..360
    private float sat = 1f;     // 0..1
    private float val = 1f;     // 0..1
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cross = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap cacheBmp;
    private Canvas cacheCanvas;
    private OnSVChange listener;

    public SaturationValueView(Context c, @Nullable AttributeSet a){ super(c,a);
        cross.setStyle(Paint.Style.STROKE);
        cross.setStrokeWidth(3f);
        cross.setColor(Color.WHITE);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setOnSVChange(OnSVChange l){ this.listener = l; }
    public void setHue(float h){ this.hue = h; invalidateCache(); invalidate(); }
    public void setSV(float s, float v){ this.sat = clamp(s); this.val = clamp(v); invalidate(); }
    public float getS(){ return sat; } public float getV(){ return val; }

    private float clamp(float x){ return Math.max(0f, Math.min(1f, x)); }

    private void invalidateCache(){ if (cacheBmp != null){ cacheBmp.recycle(); cacheBmp = null; cacheCanvas = null; } }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w,h,oldw,oldh); invalidateCache();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        final int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (cacheBmp == null){
            cacheBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            cacheCanvas = new Canvas(cacheBmp);

            // 1) Lớp SATURATION: trái (trắng) -> phải (màu theo H với S=1,V=1)
            int base = Color.HSVToColor(new float[]{hue, 1f, 1f});
            Paint p1 = new Paint(Paint.ANTI_ALIAS_FLAG);
            p1.setShader(new LinearGradient(0, 0, w, 0,
                    0xFFFFFFFF, base, Shader.TileMode.CLAMP));
            cacheCanvas.drawRect(0, 0, w, h, p1);

            // 2) Lớp VALUE: trên (trong suốt) -> dưới (đen)
            //  Lưu ý: dùng 0x00000000 (transparent) chứ KHÔNG phải 0x00FFFFFF
            Paint p2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            p2.setShader(new LinearGradient(0, 0, 0, h,
                    0x00000000, 0xFF000000, Shader.TileMode.CLAMP));
            cacheCanvas.drawRect(0, 0, w, h, p2);
        }

        c.drawBitmap(cacheBmp, 0, 0, null);

        // con trỏ
        float x = sat * w;
        float y = (1f - val) * h;
        c.drawCircle(x, y, 12f, cross);
        cross.setColor(0x80000000); c.drawCircle(x, y, 14f, cross);
        cross.setColor(Color.WHITE);
    }


    @Override public boolean onTouchEvent(MotionEvent e){
        float x = e.getX(), y = e.getY();
        sat = clamp(x / Math.max(1f, getWidth()));
        val = clamp(1f - y / Math.max(1f, getHeight()));
        if (listener != null) listener.onChange(sat, val);
        invalidate();
        return true;
    }
}
