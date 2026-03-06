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
package de.devknochen.descriptive.client.animation.animations;

import de.devknochen.descriptive.client.animation.Animation;
import de.devknochen.descriptive.client.animation.ColorUtil;

public class WaveAnimation implements Animation {

    @Override
    public int getColor(int charIndex, int totalChars, float time, int baseColor, java.util.List<Integer> gradientColors) {
        float wave = (float) Math.sin((time / 20f) + (charIndex * 0.5f));
        float progress = (wave + 1) / 2f;

        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;

        int dimR = (int)(r * 0.5f);
        int dimG = (int)(g * 0.5f);
        int dimB = (int)(b * 0.5f);

        return ColorUtil.interpolate(
                (dimR << 16) | (dimG << 8) | dimB,
                baseColor,
                progress
        );
    }

    @Override
    public String getName() { return "Wave"; }

    @Override
    public String getDescription() { return "Smooth wave effect"; }
}