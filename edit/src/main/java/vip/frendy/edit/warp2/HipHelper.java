package vip.frendy.edit.warp2;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.view.MotionEvent;

public class HipHelper implements CanvasView.OnCanvasChangeListener {
    private static final String TAG = HipHelper.class.getSimpleName();

    // Mesh size
    private static final int WIDTH = 15;
    private static final int HEIGHT = 15;
    private static final int COUNT = (WIDTH + 1) * (HEIGHT + 1);

    private MorphMatrix mMorphMatrix = new MorphMatrix(COUNT * 2);
    private MorphMatrix mMorphMatrixOrig = new MorphMatrix(COUNT * 2);
    private final Matrix mMatrix = new Matrix();
    private final Matrix mInverse = new Matrix();

    private BitmapDrawable mBitmap;
    private CanvasView mCanvasView;
    private boolean attached = false;
    private boolean visible = true;

    private RectF mOval = new RectF();
    private RectF mRectOp = new RectF();
    private Bitmap mBitmapOval, mBitmapOp;
    private boolean isSelectedOval = false;
    private boolean isSelectedOp = false;
    private float x_1, y_1;
    private float op_x, op_y;
    private float op_scale = 1;

    public void initMorpher() {
        if (mCanvasView != null) {
            mCanvasView.setFocusable(true);

            mBitmap = (BitmapDrawable) mCanvasView.getBackground();

            float w = mCanvasView.getWidth();
            float h = mCanvasView.getHeight();

            // Constructing mesh
            int index = 0;
            for (int y = 0; y <= HEIGHT; y++) {
                float fy = h * y / HEIGHT;
                for (int x = 0; x <= WIDTH; x++) {
                    float fx = w * x / WIDTH;
                    setXY(mMorphMatrix, index, fx, fy);
                    setXY(mMorphMatrixOrig, index, fx, fy);
                    index += 1;
                }
            }

            mMatrix.invert(mInverse);

            //初始化位置
            x_1 = mCanvasView.getWidth() / 2;
            y_1 = mCanvasView.getHeight() / 2;
            invalidate();
        }
    }

    public boolean isAttached() {
        return this.attached;
    }

    public void setDrawingView(CanvasView canvasView) {
        if (canvasView == null) {
            if (mCanvasView != null) {
                mCanvasView.setOnCanvasChangeListener(null);
            }
            attached = false;
        } else {
            attached = true;
            canvasView.setOnCanvasChangeListener(this);
        }
        mCanvasView = canvasView;
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFCCCCCC);

        canvas.concat(mMatrix);
        canvas.drawBitmapMesh(mBitmap.getBitmap(), WIDTH, HEIGHT, mMorphMatrix.getVerts(), 0,
                null, 0, null);

        if(mBitmapOval != null && visible) {
            float width = mBitmapOval.getWidth() * op_scale;
            float height = mBitmapOval.getHeight() * op_scale;
            mOval.left = x_1 - width / 2;
            mOval.top = y_1 - height / 2;
            mOval.right = mOval.left + width;
            mOval.bottom = mOval.top + height;
            canvas.drawBitmap(mBitmapOval, null, mOval, null);
        }
        if(mBitmapOp != null && visible) {
            mRectOp.left = mOval.right - mBitmapOp.getWidth() / 2;
            mRectOp.top = mOval.bottom - mBitmapOp.getWidth() / 2;
            mRectOp.right = mRectOp.left + mBitmapOp.getWidth();
            mRectOp.bottom = mRectOp.top + mBitmapOp.getHeight();
            canvas.drawBitmap(mBitmapOp, null, mRectOp, null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(isInArea(event.getX(), event.getY(), mOval)) {
                    isSelectedOval = true;
                    isSelectedOp = false;
                    x_1 = event.getX();
                    y_1 = event.getY();
                    invalidate();
                } else if(mBitmapOp != null && isInArea(event.getX(), event.getY(), mRectOp)) {
                    isSelectedOval = false;
                    isSelectedOp = true;
                    op_x = event.getX();
                    op_y = event.getY();
                } else {
                    isSelectedOval = false;
                    isSelectedOp = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(isSelectedOval) {
                    x_1 = event.getX();
                    y_1 = event.getY();
                    invalidate();
                } else if(isSelectedOp) {
                    double d = event.getX() - op_x;
                    op_scale = 1 + (float) d / mCanvasView.getWidth();
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    @Override
    public void onPreGenerateBitmap() {
        visible = false;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    //作用范围半径
    private int r = 150;
    private void warp(float startX, float startY, float endX, float endY) {
        //计算拖动距离
        float ddPull = (endX - startX) * (endX - startX) + (endY - startY) * (endY - startY);
        float dPull = (float) Math.sqrt(ddPull);
        //文献中提到的算法，并不能很好的实现拖动距离 MC 越大变形效果越明显的功能，下面这行代码则是我对该算法的优化
        //dPull = screenWidth - dPull >= 0.0001f ? screenWidth - dPull : 0.0001f;

        float[] orig = mMorphMatrixOrig.getVerts();
        float[] verts = mMorphMatrix.getVerts();

        for (int i = 0; i < COUNT * 2; i += 2) {
            //计算每个坐标点与触摸点之间的距离
            float dx = orig[i] - startX;
            float dy = orig[i + 1] - startY;
            float dd = dx * dx + dy * dy;
            float d = (float) Math.sqrt(dd);

            //文献中提到的算法同样不能实现只有圆形选区内的图像才进行变形的功能，这里需要做一个距离的判断
            if (d < r) {
                //变形系数，扭曲度
                double e = (r * r - dd) * (r * r - dd) / ((r * r - dd + dPull * dPull) * (r * r - dd + dPull * dPull));
                double pullX = e * (endX - startX);
                double pullY = e * (endY - startY);
                verts[i] = (float) (orig[i] + pullX);
                verts[i + 1] = (float) (orig[i + 1] + pullY);
            }
        }
        invalidate();
    }

    private void toWarpLeft(int strength) {
        float _startX = mOval.left;
        float _startY = mOval.top;
        int _step = 1;
        int _step_max = (int)(mOval.bottom - mOval.top);

        while (_step < _step_max) {
            float _scale = 1;//Math.abs(_step_max / 2 - _step) / _step_max / 2;
            float _endX = _startX + strength * _scale;
            float _endY = _startY;

            warp(_startX, _startY, _endX, _endY);

            _startY += 1;
            _step += 1;
        }
    }

    private void toWarpRight(int strength) {
        float _startX = mOval.right;
        float _startY = mOval.top;
        int _step = 1;
        int _step_max = (int)(mOval.bottom - mOval.top);

        while (_step < _step_max) {
            float _scale = 1;//Math.abs(_step_max / 2 - _step) / _step_max / 2;
            float _endX = _startX - strength * _scale;
            float _endY = _startY;

            warp(_startX, _startY, _endX, _endY);

            _startY += 1;
            _step += 1;
        }
    }

    public void invalidate() {
        if (mCanvasView != null) {
            mCanvasView.invalidate();
        }
    }

    public BitmapDrawable getBitmapDrawable() {
        return mBitmap;
    }

    public void setOvalBitmap(Bitmap oval) {
        mBitmapOval = oval;
    }

    public void setOpBitmap(Bitmap op) {
        mBitmapOp = op;
    }

    public void setStrength(int strength) {
        toWarpLeft(strength);
        toWarpRight(strength);
    }

    private boolean isInArea(float x, float y, RectF rectF) {
        return (x >= rectF.left && x <= rectF.right && y >= rectF.top && y <= rectF.bottom);
    }

    private static void setXY(MorphMatrix morphMatrix, int index, float x, float y) {
        morphMatrix.getVerts()[index * 2] = x;
        morphMatrix.getVerts()[index * 2 + 1] = y;
    }

    private static class MorphMatrix {
        private float[] verts;

        public MorphMatrix(MorphMatrix morphMatrix) {
            this.verts = new float[morphMatrix.verts.length];
            System.arraycopy(morphMatrix.verts, 0, this.verts, 0, morphMatrix.verts.length);
        }

        public MorphMatrix(final int size) {
            verts = new float[size];
        }

        public float[] getVerts() {
            return verts;
        }

        public void set(MorphMatrix morphMatrix) {
            System.arraycopy(morphMatrix.verts, 0, this.verts, 0, morphMatrix.verts.length);
        }
    }
}