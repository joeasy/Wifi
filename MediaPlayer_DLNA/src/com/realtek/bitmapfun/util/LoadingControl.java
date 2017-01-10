
package com.realtek.bitmapfun.util;

public interface LoadingControl
{
    void startLoading(int pos);

    void stopLoading(int pos,boolean isFromonCancel);
}
