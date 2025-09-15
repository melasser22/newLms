
package com.ejada.actuator.starter.info;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public class SharedInfoContributor implements InfoContributor {

  @Autowired(required = false)
  private ObjectProvider<BuildProperties> build;

  @Autowired(required = false)
  private ObjectProvider<GitProperties> git;

  @Override
  public void contribute(Info.Builder builder) {
    Map<String, Object> shared = new LinkedHashMap<>();
    BuildProperties bp = build != null ? build.getIfAvailable() : null;
    if (bp != null) {
      shared.put("app", bp.getName());
      shared.put("version", bp.getVersion());
      shared.put("time", bp.getTime());
    }
    GitProperties gp = git != null ? git.getIfAvailable() : null;
    if (gp != null) {
      shared.put("git-commit", gp.getShortCommitId());
      shared.put("git-branch", gp.getBranch());
    }
    builder.withDetail("shared", shared);
  }
}
