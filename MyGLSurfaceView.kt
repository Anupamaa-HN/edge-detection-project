package com.example.edgeviewer

import android.content.Context
import android.util.AttributeSet
import android.view.Surface
import android.opengl.GLSurfaceView

class MyGLSurfaceView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : GLSurfaceView(ctx, attrs) {
    var renderer: EdgeRenderer? = null
    init {
        setEGLContextClientVersion(2)
        renderer = EdgeRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }
    fun getSurface(): Surface {
        return renderer!!.getSurface()
    }
}
