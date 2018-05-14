package cc.openframeworks.planear;
import android.view.View;

public class OFActivity extends cc.openframeworks.OFActivity{

    public static native void clearDrawing();
    public void clear(View b){ clearDrawing(); }
}

