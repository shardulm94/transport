/**
 * Copyright 2019 LinkedIn Corporation. All rights reserved.
 * Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.transport.plugin.packaging;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.linkedin.transport.plugin.Platform;
import java.util.List;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.distribution.DistributionContainer;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.Tar;
import org.gradle.api.tasks.bundling.Zip;

import static com.linkedin.transport.plugin.ConfigurationType.*;
import static com.linkedin.transport.plugin.ConfigurationUtils.*;
import static com.linkedin.transport.plugin.SourceSetUtils.*;


/**
 * A {@link Packaging} class which generates distribution TARs and ZIPs containing all runtime dependencies using the
 * {@link org.gradle.api.distribution.plugins.DistributionPlugin}
 */
public class DistributionPackaging implements Packaging {

  private final String _classifier;
  private final Set<String> _excludedDependencies;

  public DistributionPackaging() {
    this("", ImmutableSet.of());
  }

  public DistributionPackaging(String classifier, Set<String> excludedDependencies) {
    _classifier = classifier;
    _excludedDependencies = excludedDependencies;
  }

  @Override
  public List<TaskProvider<? extends Task>> configurePackagingTasks(Project project, Platform platform,
      SourceSet platformSourceSet, SourceSet mainSourceSet) {
    String platformWithClassifier = platform.getName() + _classifier;
    // Create a thin JAR to be included in the distribution
    final TaskProvider<Jar> platformThinJarTask = createThinJarTask(project, platformSourceSet, platform.getName(),
        platformWithClassifier);

    /*
      Include the thin JAR and all the runtime dependencies into the distribution for a given platform

      distributions {
        <platformName> {
          contents {
            from <platformThinJarTask>
            from project.configurations.<platformRuntimeClasspath>
          }
        }
      }
     */
    DistributionContainer distributions = project.getExtensions().getByType(DistributionContainer.class);
    distributions.register(platformWithClassifier, distribution -> {
      distribution.setBaseName(project.getName());
      distribution.getContents()
          .from(platformThinJarTask)
          .from(applyExcludes(project,
              getConfigurationForSourceSet(project, platformSourceSet, RUNTIME_CLASSPATH),
              _excludedDependencies));
    });

    // Explicitly set classifiers for the created distributions or else leads to Maven packaging issues due to multiple
    // artifacts with the same classifier
    project.getTasks().named(platformWithClassifier + "DistTar", Tar.class,
        tar -> tar.setClassifier(platformWithClassifier));
    project.getTasks().named(platformWithClassifier + "DistZip", Zip.class,
        zip -> zip.setClassifier(platformWithClassifier));
    return ImmutableList.of(project.getTasks().named(platformWithClassifier + "DistTar", Tar.class),
        project.getTasks().named(platformWithClassifier + "DistZip", Zip.class));
  }

  /**
   * Creates a thin JAR for the platform's {@link SourceSet} to be included in the distribution
   */
  private TaskProvider<Jar> createThinJarTask(Project project, SourceSet sourceSet, String platformName,
      String platformWithClassifier) {
      /*
        task <platformName>ThinJar(type: Jar, dependsOn: prestoClasses) {
          classifier '<platformWithClassifier>Thin'
          from sourceSets.<platform>.output
          from sourceSets.<platform>.resources
        }
      */

    return project.getTasks().register(platformWithClassifier + "ThinJar", Jar.class, task -> {
      task.dependsOn(project.getTasks().named(sourceSet.getClassesTaskName()));
      task.setDescription("Assembles a thin jar archive containing the " + platformName
          + " classes to be included in the " + platformWithClassifier + " distribution");
      task.setClassifier(platformWithClassifier + "Thin");
      task.from(sourceSet.getOutput());
      task.from(sourceSet.getResources());
    });
  }
}
