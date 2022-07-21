/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tw.gasol.android.grafika.gles;

import android.opengl.Matrix;

/**
 * This class essentially represents a viewport-sized sprite that will be rendered with
 * a texture, usually from an external source like the camera or video decoder.
 */
public class FullFrameRect {
    private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE);
    private Texture2dProgram mProgram;
    private float mAngle;

    // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
    private float[] mMvpMatrix = GlUtil.IDENTITY_MATRIX;
    private boolean mMatrixReady = true;

    /**
     * Prepares the object.
     *
     * @param program The program to use.  FullFrameRect takes ownership, and will release
     *     the program when no longer needed.
     */
    public FullFrameRect(Texture2dProgram program) {
        mProgram = program;
    }

    /**
     * Sets the full frame rotation angle, in degrees. Rect will rotate counter-clockwise.
     */
    public void setRotation(float angle) {
        // Normalize.  We're not expecting it to be way off, so just iterate.
        while (angle >= 360.0f) {
            angle -= 360.0f;
        }
        while (angle <= -360.0f) {
            angle += 360.0f;
        }
        mAngle = angle;
        mMatrixReady = false;
    }

    /**
     * Returns the model-view matrix.
     * <p>
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    public float[] getModelViewMatrix() {
        if (!mMatrixReady) {
            if (mAngle != 0.0f) {
                Matrix.rotateM(mMvpMatrix, 0, mAngle, 0.0f, 0.0f, 1.0f);
            }
            mMatrixReady = true;
        }
        return mMvpMatrix;
    }

    /**
     * Releases resources.
     * <p>
     * This must be called with the appropriate EGL context current (i.e. the one that was
     * current when the constructor was called).  If we're about to destroy the EGL context,
     * there's no value in having the caller make it current just to do this cleanup, so you
     * can pass a flag that will tell this function to skip any EGL-context-specific cleanup.
     */
    public void release(boolean doEglCleanup) {
        if (mProgram != null) {
            if (doEglCleanup) {
                mProgram.release();
            }
            mProgram = null;
        }
    }

    /**
     * Returns the program currently in use.
     */
    public Texture2dProgram getProgram() {
        return mProgram;
    }

    /**
     * Changes the program.  The previous program will be released.
     * <p>
     * The appropriate EGL context must be current.
     */
    public void changeProgram(Texture2dProgram program) {
        mProgram.release();
        mProgram = program;
    }

    /**
     * Creates a texture object suitable for use with drawFrame().
     */
    public int createTextureObject() {
        return mProgram.createTextureObject();
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object.
     */
    public void drawFrame(int textureId, float[] texMatrix) {
        mProgram.draw(getModelViewMatrix(), mRectDrawable.getVertexArray(), 0,
                mRectDrawable.getVertexCount(), mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix, mRectDrawable.getTexCoordArray(), textureId,
                mRectDrawable.getTexCoordStride());
    }
}
