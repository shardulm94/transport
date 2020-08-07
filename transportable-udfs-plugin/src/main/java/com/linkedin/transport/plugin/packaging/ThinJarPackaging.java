/**
 * Copyright 2019 LinkedIn Corporation. All rights reserved.
 * Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.transport.plugin.packaging;

import com.google.common.collect.ImmutableList;
import com.linkedin.transport.plugin.Platform;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;


public class ThinJarPackaging implements Packaging {

  @Override
  public List<TaskProvider<? extends Task>> configurePackagingTasks(Project project, Platform platform,
      SourceSet platformSourceSet, SourceSet mainSourceSet) {
    TaskProvider<? extends Task> thinJarTask =
        project.getTasks().register(platformSourceSet.getTaskName(null, "thinJar"), Jar.class, task -> {
          task.dependsOn(project.getTasks().named(platformSourceSet.getClassesTaskName()));
          task.setDescription("Assembles a thin jar archive containing the " + platform.getName()
              + " classes to be included in the distribution");
          task.setClassifier(platform.getName() + "Thin");
          task.from(platformSourceSet.getOutput());
          task.from(platformSourceSet.getResources());
        });

    project.getArtifacts().add(Dependency.ARCHIVES_CONFIGURATION, thinJarTask);
    return ImmutableList.of(thinJarTask);
  }
}
