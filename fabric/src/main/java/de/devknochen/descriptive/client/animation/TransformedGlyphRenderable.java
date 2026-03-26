/*
 * Copyright 2026 DevKnochen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.devknochen.descriptive.client.animation;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class TransformedGlyphRenderable implements TextRenderable.Styled {

    private final TextRenderable.Styled delegate;
    private final float rotationRadians;
    private final float pivotXFactor;
    private final float pivotYFactor;

    public TransformedGlyphRenderable(TextRenderable.Styled delegate, float rotationRadians,
                                      float pivotXFactor, float pivotYFactor) {
        this.delegate = delegate;
        this.rotationRadians = rotationRadians;
        this.pivotXFactor = pivotXFactor;
        this.pivotYFactor = pivotYFactor;
    }

    @Override
    public void render(Matrix4fc matrix, VertexConsumer vertexConsumer, int light, boolean shadow) {
        if (rotationRadians == 0.0f) {
            delegate.render(matrix, vertexConsumer, light, shadow);
            return;
        }

        float pivotX = left() + (right() - left()) * pivotXFactor;
        float pivotY = top() + (bottom() - top()) * pivotYFactor;

        Matrix4f transformed = new Matrix4f(matrix)
                .translate(pivotX, pivotY, 0.0f)
                .rotateZ(rotationRadians)
                .translate(-pivotX, -pivotY, 0.0f);
        delegate.render(transformed, vertexConsumer, light, shadow);
    }

    @Override
    public RenderType renderType(Font.DisplayMode displayMode) {
        return delegate.renderType(displayMode);
    }

    @Override
    public GpuTextureView textureView() {
        return delegate.textureView();
    }

    @Override
    public RenderPipeline guiPipeline() {
        return delegate.guiPipeline();
    }

    @Override
    public float left() {
        return delegate.left();
    }

    @Override
    public float top() {
        return delegate.top();
    }

    @Override
    public float right() {
        return delegate.right();
    }

    @Override
    public float bottom() {
        return delegate.bottom();
    }

    @Override
    public Style style() {
        return delegate.style();
    }

    @Override
    public float activeLeft() {
        return delegate.activeLeft();
    }

    @Override
    public float activeTop() {
        return delegate.activeTop();
    }

    @Override
    public float activeRight() {
        return delegate.activeRight();
    }

    @Override
    public float activeBottom() {
        return delegate.activeBottom();
    }
}
