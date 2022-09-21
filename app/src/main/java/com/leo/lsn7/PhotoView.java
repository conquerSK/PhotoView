package com.leo.lsn7;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.Nullable;

public class PhotoView extends View {
    private Bitmap bitmap;
    private Paint paint;

    // 偏移值
    private float originalOffsetX;
    private float originalOffsetY;

    // 一边全屏，一边留白
    private float smallScale;
    // 一边全屏，一边超出屏幕
    private float bigScale;

    private float OVER_SCALE_FACTOR = 1.5f;

    private float currentScale;

    private boolean isEnlarge;

    private GestureDetector gestureDetector;

    private float offsetX;
    private float offsetY;

    private OverScroller overScroller;

    private FlingRunner flingRunner;

    private ScaleGestureDetector scaleGestureDetector;

    private boolean isScale;

    public PhotoView(Context context) {
        this(context, null);
    }

    public PhotoView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 获取bitmap对象
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.photo);
        paint = new Paint();

        gestureDetector = new GestureDetector(context, new PhotoGestureListener());

        overScroller = new OverScroller(context);

        flingRunner = new FlingRunner();

        scaleGestureDetector = new ScaleGestureDetector(context, new PhotoScaleGestureListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 双指操作优先
        boolean result = scaleGestureDetector.onTouchEvent(event);
        if (!scaleGestureDetector.isInProgress()) {
            result = gestureDetector.onTouchEvent(event);
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float scaleFraction = (currentScale - smallScale) / (bigScale - smallScale);
        canvas.translate(offsetX * scaleFraction, offsetY * scaleFraction);

        // smallScale --》 bigScale
        canvas.scale(currentScale, currentScale, getWidth() / 2f, getHeight() / 2f);

        // 绘制bitmap
        canvas.drawBitmap(bitmap, originalOffsetX, originalOffsetY, paint);
    }

    // onMeasure --> onSizeChanged
    // 每次改变尺寸时也会调用
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 需要得到 浮点数，否则会留条小缝
        originalOffsetX = (getWidth() - bitmap.getWidth()) / 2f;
        originalOffsetY = (getHeight() - bitmap.getHeight()) / 2f;

        // 图片是横向的
        if ((float) bitmap.getWidth() / bitmap.getHeight() > (float) getWidth() / getHeight()) {
            smallScale = (float) getWidth() / bitmap.getWidth();
            bigScale = (float) getHeight() / bitmap.getHeight() * OVER_SCALE_FACTOR;
        } else { // 纵向的图片
            smallScale = (float) getHeight() / bitmap.getHeight();
            bigScale = (float) getWidth() / bitmap.getWidth() * OVER_SCALE_FACTOR;
        }
        currentScale = smallScale;
    }

    class PhotoGestureListener extends GestureDetector.SimpleOnGestureListener {
        // Up时触发  双击的时候，触发两次？？第二次抬起时触发
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return super.onSingleTapUp(e);
        }

        // 长按 -- 300ms
        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }

        /**
         * 类似move事件
         *
         * @param e1
         * @param e2
         * @param distanceX 在 X 轴上滑过的距离（单位时间） 旧位置 - 新位置
         * @param distanceY
         * @return
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // 只有在放大的情况下，才能进行移动
            if (isEnlarge) {
                offsetX -= distanceX;
                offsetY -= distanceY;
                fixOffsets();
                invalidate();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        // 抛掷
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (isEnlarge) {
                // 只会处理一次
                overScroller.fling((int) offsetX, (int) offsetY, (int) velocityX, (int) velocityY,
                        -(int) (bitmap.getWidth() * bigScale - getWidth()) / 2,
                        (int) (bitmap.getWidth() * bigScale - getWidth()) / 2,
                        -(int) (bitmap.getHeight() * bigScale - getHeight()) / 2,
                        (int) (bitmap.getHeight() * bigScale - getHeight()) / 2, 600, 600);
                postOnAnimation(flingRunner);
            }

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        // 延时触发 100ms -- 点击效果，水波纹
        @Override
        public void onShowPress(MotionEvent e) {
            super.onShowPress(e);
        }

        // 按下 -- 注意：直接返回true
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        // 双击 -- 第二次点击按下的时候 -- 40ms（小于表示：防抖动） -- 300ms
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            isEnlarge = !isEnlarge;
            if (isEnlarge) {
                offsetX = (e.getX() - getWidth() / 2f) - (e.getX() - getWidth() / 2) * bigScale / smallScale;
                offsetY = (e.getY() - getHeight() / 2f) - (e.getY() - getHeight() / 2) * bigScale / smallScale;
                fixOffsets();
                // 启动属性动画
                getScaleAnimator().start();
            } else {
                getScaleAnimator().reverse();
            }
//            invalidate();
            return super.onDoubleTap(e);
        }

        // 双击第二次 down、move、up都会触发
        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return super.onDoubleTapEvent(e);
        }

        // 单击按下时触发，双击时不触发，
        // 延时300ms触发TAP事件
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }
    }

    class FlingRunner implements Runnable {

        @Override
        public void run() {
            // 动画还在执行 则返回true
            if (overScroller.computeScrollOffset()) {
                offsetX = overScroller.getCurrX();
                offsetY = overScroller.getCurrY();
                invalidate();
                // 没帧动画执行一次，性能更好
                postOnAnimation(this);
            }
        }
    }

    private void fixOffsets() {
        offsetX = Math.min(offsetX, (bitmap.getWidth() * bigScale - getWidth()) / 2);
        offsetX = Math.max(offsetX, -(bitmap.getWidth() * bigScale - getWidth()) / 2);
        offsetY = Math.min(offsetY, (bitmap.getHeight() * bigScale - getHeight()) / 2);
        offsetY = Math.max(offsetY, -(bitmap.getHeight() * bigScale - getHeight()) / 2);
    }

    /**
     * 属性动画，设置放大缩小的效果
     */
    private ObjectAnimator scaleAnimator;

    private ObjectAnimator getScaleAnimator() {
        if (scaleAnimator == null) {
            scaleAnimator = ObjectAnimator.ofFloat(this, "currentScale", 0);
        }
        if (isScale) {
            isScale = false;
            scaleAnimator.setFloatValues(smallScale, currentScale);
        } else {
//             放大缩小的范围
            scaleAnimator.setFloatValues(smallScale, bigScale);
        }
        return scaleAnimator;
    }

    // 属性动画，值会不断地从 smallScale 慢慢 加到 bigScale， 通过反射调用改方法
    public void setCurrentScale(float currentScale) {
        this.currentScale = currentScale;
        invalidate();
    }

    class PhotoScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener {

        float initialScale;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if ((currentScale > smallScale && !isEnlarge)
                    || (currentScale == smallScale && isEnlarge)) {
                isEnlarge = !isEnlarge;
            }
            currentScale = initialScale * detector.getScaleFactor();
            isScale = true;
            invalidate();
            return false;
        }

        // 注意：返回true，消费事件
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            initialScale = currentScale;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    }


}
