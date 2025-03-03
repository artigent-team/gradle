/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal.artifacts.configurations;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.internal.artifacts.ResolveContext;
import org.gradle.api.internal.artifacts.transform.ExtraExecutionGraphDependenciesResolverFactory;
import org.gradle.api.internal.attributes.AttributeContainerInternal;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.internal.DisplayName;
import org.gradle.internal.FinalizableValue;
import org.gradle.internal.deprecation.DeprecatableConfiguration;
import org.gradle.util.Path;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public interface ConfigurationInternal extends ResolveContext, Configuration, DeprecatableConfiguration, DependencyMetaDataProvider, FinalizableValue {
    enum InternalState {
        UNRESOLVED,
        BUILD_DEPENDENCIES_RESOLVED,
        GRAPH_RESOLVED,
        ARTIFACTS_RESOLVED
    }

    @Override
    ResolutionStrategyInternal getResolutionStrategy();

    @Override
    AttributeContainerInternal getAttributes();

    String getPath();

    Path getIdentityPath();

    void setReturnAllVariants(boolean returnAllVariants);

    boolean getReturnAllVariants();

    /**
     * Runs any registered dependency actions for this Configuration, and any parent Configuration.
     * Actions may mutate the dependency set for this configuration.
     * After execution, all actions are de-registered, so execution will only occur once.
     */
    void runDependencyActions();

    void markAsObserved(InternalState requestedState);

    void addMutationValidator(MutationValidator validator);

    void removeMutationValidator(MutationValidator validator);

    /**
     * Converts this configuration to an {@link OutgoingVariant} view. The view may not necessarily be immutable.
     */
    OutgoingVariant convertToOutgoingVariant();

    /**
     * Visits the variants of this configuration.
     */
    void collectVariants(VariantVisitor visitor);

    /**
     * Registers an action to execute before locking for further mutation.
     */
    void beforeLocking(Action<? super ConfigurationInternal> action);

    boolean isCanBeMutated();

    /**
     * Locks the configuration for mutation
     * <p>
     * Any invalid state at this point will be added to the returned list of exceptions.
     * Handling these becomes the responsibility of the caller.
     *
     * @return a list of validation failures when not empty
     */
    List<? extends GradleException> preventFromFurtherMutationLenient();

    /**
     * Reports whether this configuration uses {@link org.gradle.api.Incubating Incubating} attributes types, such as {@link org.gradle.api.attributes.Category#VERIFICATION}.
     * @return
     */
    boolean isIncubating();

    /**
     * Gets the complete set of exclude rules including those contributed by
     * superconfigurations.
     */
    Set<ExcludeRule> getAllExcludeRules();

    ExtraExecutionGraphDependenciesResolverFactory getDependenciesResolver();

    @Nullable
    ConfigurationInternal getConsistentResolutionSource();

    Supplier<List<DependencyConstraint>> getConsistentResolutionConstraints();

    /**
     * Decorates a resolve exception with more context. This can be used
     * to give hints to the user when a resolution error happens.
     * @param e a resolve exception
     * @return a decorated resolve exception, or the same exception
     */
    ResolveException maybeAddContext(ResolveException e);

    /**
     * Test if this configuration can either be declared against or extends another
     * configuration which can be declared against.
     *
     * @return {@code true} if so; {@code false} otherwise
     */
    default boolean isDeclarableAgainstByExtension() {
        return isDeclarableAgainstByExtension(this);
    }

    /**
     * Configures if a configuration can have dependencies declared upon it.
     *
     */
    void setCanBeDeclaredAgainst(boolean allowed);

    /**
     * Returns true if it is allowed to declare dependencies upon this configuration.
     * Defaults to true.
     * @return true if this configuration can have dependencies declared
     */
    boolean isCanBeDeclaredAgainst();

    /**
     * Prevents any calls to methods that change this configuration's allowed usage (e.g. {@link #setCanBeConsumed(boolean)},
     * {@link #setCanBeResolved(boolean)}, {@link #deprecateForResolution(String...)}) from succeeding; and causes them
     * to throw an exception.
     */
    void preventUsageMutation();

    /**
     * Returns the role used to create this configuration and set its initial allowed usage.
     */
    ConfigurationRole getRoleAtCreation();

    /**
     * Test if the given configuration can either be declared against or extends another
     * configuration which can be declared against.
     * This method should probably be made {@code private} when upgrading to Java 9.
     *
     * @param configuration the configuration to test
     * @return {@code true} if so; {@code false} otherwise
     */
    static boolean isDeclarableAgainstByExtension(ConfigurationInternal configuration) {
        if (configuration.isCanBeDeclaredAgainst()) {
            return true;
        } else {
            return configuration.getExtendsFrom().stream()
                    .map(ConfigurationInternal.class::cast)
                    .anyMatch(ci -> ci.isDeclarableAgainstByExtension());
        }
    }

    interface VariantVisitor {
        // The artifacts to use when this configuration is used as a configuration
        void visitArtifacts(Collection<? extends PublishArtifact> artifacts);

        // This configuration as a variant. May not always be present
        void visitOwnVariant(DisplayName displayName, ImmutableAttributes attributes, Collection<? extends Capability> capabilities, Collection<? extends PublishArtifact> artifacts);

        // A child variant. May not always be present
        void visitChildVariant(String name, DisplayName displayName, ImmutableAttributes attributes, Collection<? extends Capability> capabilities, Collection<? extends PublishArtifact> artifacts);
    }
}
