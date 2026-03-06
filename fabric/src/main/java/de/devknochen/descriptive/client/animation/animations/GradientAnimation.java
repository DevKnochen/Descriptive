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

import java.util.List;

public class GradientAnimation implements Animation {

    @Override
    public int getColor(int charIndex, int totalChars, float time, int baseColor, List<Integer> gradientColors) {
        if (gradientColors == null || gradientColors.size() < 2) return baseColor;

        int segments = gradientColors.size() - 1;
        float t = ((float) charIndex / Math.max(totalChars - 1, 1)) * segments;
        int segment = (int) t;
        if (segment >= segments) segment = segments - 1;
        float localT = t - segment;

        int c1 = gradientColors.get(segment);
        int c2 = gradientColors.get(segment + 1);

        int r = (int) (((c1 >> 16) & 0xFF) * (1 - localT) + ((c2 >> 16) & 0xFF) * localT);
        int g = (int) (((c1 >> 8)  & 0xFF) * (1 - localT) + ((c2 >> 8)  & 0xFF) * localT);
        int b = (int) ((c1 & 0xFF) * (1 - localT) + (c2 & 0xFF) * localT);

        return (r << 16) | (g << 8) | b;
    }

    @Override public String getName()        { return "Gradient"; }
    @Override public String getDescription() { return "Gradient color across name"; }
}