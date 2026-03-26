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

public class EffectSettings {
    public final int index;
    public float offsetX;
    public float offsetY;
    public float rotationRadians;
    public float pivotXFactor;
    public float pivotYFactor;
    public float r;
    public float g;
    public float b;
    public float a;

    public EffectSettings(int index) {
        this.index   = index;
        this.offsetX = 0;
        this.offsetY = 0;
        this.rotationRadians = 0;
        this.pivotXFactor = 0.5f;
        this.pivotYFactor = 0.5f;
        this.r       = 1.0f;
        this.g       = 1.0f;
        this.b       = 1.0f;
        this.a       = 1.0f;
    }
}
