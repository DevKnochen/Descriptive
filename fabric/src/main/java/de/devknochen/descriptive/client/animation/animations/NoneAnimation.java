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

public class NoneAnimation implements Animation {

    @Override
    public int getColor(int charIndex, int totalChars, float time, int baseColor, java.util.List<Integer> gradientColors) {
        return baseColor;
    }

    @Override
    public String getName() { return "None"; }

    @Override
    public String getDescription() {
        return "No animation";
    }
}