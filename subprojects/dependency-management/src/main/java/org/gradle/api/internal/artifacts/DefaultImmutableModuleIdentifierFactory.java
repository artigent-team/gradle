/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.api.internal.artifacts;

import com.google.common.collect.Maps;
import org.gradle.api.artifacts.ModuleIdentifier;

import java.util.Map;

public class DefaultImmutableModuleIdentifierFactory implements ImmutableModuleIdentifierFactory {
    private final Map<String, Map<String, ModuleIdentifier>> byGroup = Maps.newConcurrentMap();

    @Override
    public ModuleIdentifier module(String group, String name) {
        Map<String, ModuleIdentifier> byName = byGroup.get(group);
        if (byName == null) {
            byName = Maps.newConcurrentMap();
            byGroup.put(group, byName);
        }
        ModuleIdentifier moduleIdentifier = byName.get(name);
        if (moduleIdentifier == null) {
            moduleIdentifier = DefaultModuleIdentifier.newId(group, name);
            byName.put(name, moduleIdentifier);
        }
        return moduleIdentifier;
    }
}
