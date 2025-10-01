package com.trial.webview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class NonScrollableWebView extends WebView {

    public NonScrollableWebView(Context context) {
        super(context);
    }

    public NonScrollableWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Allow taps/clicks to pass through
        // Block scroll-related move events
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return true;  // consume move events to disable scrolling
        }
        return super.onTouchEvent(event);  // allow clicks, taps, etc
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        // Force stay at bottom
        scrollTo(0, computeVerticalScrollRange());
        super.onScrollChanged(l, t, oldl, oldt);
    }
}
