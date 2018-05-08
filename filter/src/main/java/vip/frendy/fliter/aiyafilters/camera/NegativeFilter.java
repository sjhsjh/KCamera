package vip.frendy.fliter.aiyafilters.camera;

import android.content.res.Resources;

/**
 * Created by Simon on 2017/7/6.
 */

public class NegativeFilter extends AFilter {

    public NegativeFilter(Resources mRes) {
        super(mRes);
    }

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/base_vertex.sh",
                "shader/color/negative_fragment.frag");
    }

    @Override
    protected void onSizeChanged(int width, int height) {
    }
}