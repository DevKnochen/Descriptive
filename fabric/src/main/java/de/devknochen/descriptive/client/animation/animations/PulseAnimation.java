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

public class PulseAnimation implements Animation {

    @Override
    public int getColor(int charIndex, int totalChars, float time, int baseColor, java.util.List<Integer> gradientColors) {
        float pulse = (float) Math.sin(time / 30f);
        float progress = (pulse + 1) / 2f;

        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;

        int dimColor = ((int)(r * 0.3f) << 16) | ((int)(g * 0.3f) << 8) | (int)(b * 0.3f);

        return ColorUtil.interpolate(dimColor, baseColor, progress);
    }

    @Override
    public String getName() { return "Pulse"; }

    @Override
    public String getDescription() { return "Breathing color effect"; }
}