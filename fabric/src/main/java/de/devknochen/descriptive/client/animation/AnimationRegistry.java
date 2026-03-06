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

import de.devknochen.descriptive.client.animation.animations.*;
import java.util.HashMap;
import java.util.Map;

public class AnimationRegistry {
    private static final Map<String, Animation> ANIMATIONS = new HashMap<>();
    static {
        register("none",     new NoneAnimation());
        register("shake",    new ShakeAnimation());
        register("wave",     new WaveAnimation());
        register("rainbow",  new RainbowAnimation());
        register("wiggle",   new WiggleAnimation());
        register("pulse",    new PulseAnimation());
        register("bounce",   new BounceAnimation());
        register("swing",    new SwingAnimation());
        register("fade",     new FadeAnimation());
        register("pend",     new PendulumAnimation());
        register("turb",     new TurbulenceAnimation());
        register("glitch",   new GlitchAnimation());
        register("gradient", new GradientAnimation());
    }
    private static void register(String id, Animation animation) {
        ANIMATIONS.put(id, animation);
    }
    public static Animation get(String id) {
        return ANIMATIONS.getOrDefault(id, ANIMATIONS.get("none"));
    }
    public static Map<String, Animation> getAll() {
        return new HashMap<>(ANIMATIONS);
    }
}