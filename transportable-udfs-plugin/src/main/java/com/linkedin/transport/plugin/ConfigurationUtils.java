/**
 * Copyright 2019 LinkedIn Corporation. All rights reserved.
 * Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.transport.plugin;

import com.google.common.collect.ImmutableMap;
import com.linkedin.transport.plugin.tasks.ShadeTask;
import java.util.Set;
import java.util.UUID;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;


public class ConfigurationUtils {
  private static final Logger LOG = Logging.getLogger(ShadeTask.class);

  private ConfigurationUtils() {
  }

  /**
   * Create a copy of the input configuration and returns the copied configuration without the excluded dependencies
   */
  public static Configuration applyExcludes(Project project, Configuration source, Set<String> excludedDependencies) {
    Configuration conf = project.getConfigurations().create(source.getName() + "_clone_" + UUID.randomUUID());
    conf.extendsFrom(source);
    excludedDependencies.forEach(artifact -> {
      int idx = artifact.indexOf(':');
      if (idx == -1) {
        LOG.info("Will exclude all artifacts having the group: " + artifact + " from the shaded jar");
        conf.exclude(ImmutableMap.of("group", artifact));
      } else {
        LOG.info("Will exclude all artifacts having the group and module: " + artifact + " from the shaded jar");
        conf.exclude(ImmutableMap.of("group", artifact.substring(0, idx), "module", artifact.substring(idx + 1)));
      }
    });
    return conf;
  }
}
