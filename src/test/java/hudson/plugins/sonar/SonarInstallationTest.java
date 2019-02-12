/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package hudson.plugins.sonar;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.Util;
import hudson.model.Run;
import hudson.plugins.sonar.model.TriggersConfig;
import java.io.File;
import java.io.IOException;

import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Evgeny Mandrikov
 */
public class SonarInstallationTest extends SonarTestCase {

  @Test
  public void testRoundtrip() throws IOException {
    TriggersConfig triggers = new TriggersConfig();
    SonarGlobalConfiguration d = new SonarGlobalConfiguration();
    SonarInstallation inst = spy(new SonarInstallation(
        "Name",
        "server.url",
        "credentialsId",
        null,
        "mojoVersion",
        "props",
        "key=value",
        triggers)
    );
    StandardCredentials cred = new UsernamePasswordCredentialsImpl(null, null, null, null, "token");
    doReturn(cred).when(inst).getCredentials(any(Run.class));
    d.setInstallations(inst);

    SonarInstallation i = new SonarGlobalConfiguration().getInstallations()[0];
    String storedConfig = Util.loadFile(new File(Jenkins.getInstance().getRootDir(), d.getId() + ".xml"));

    assertThat(i.getName()).isEqualTo("Name");
    assertThat(i.getServerUrl()).isEqualTo("server.url");
    assertThat(i.getServerAuthenticationToken(mock(Run.class))).isEqualTo("token");
    assertThat(i.getMojoVersion()).isEqualTo("mojoVersion");
    assertThat(i.getAdditionalProperties()).isEqualTo("props");
    assertThat(i.getAdditionalAnalysisProperties()).isEqualTo("key=value");

    assertThat(storedConfig).doesNotContain("dbPasswd");
    assertThat(storedConfig).doesNotContain("sonarPasswd");
  }

  @Test
  public void testRoundtripWithoutToken() throws IOException {
    TriggersConfig triggers = new TriggersConfig();
    SonarInstallation inst = new SonarInstallation(
        "Name",
        "server.url",
        null,
        null,
        "mojoVersion",
        "props",
        "key=value",
        triggers
    );
    SonarGlobalConfiguration d = new SonarGlobalConfiguration();
    d.setInstallations(inst);
    d.setInstallations(new SonarInstallation(
            "Name",
            "server.url",
            null,
            null,
            "mojoVersion",
            "props",
            "key=value",
            triggers
    )
    );
    d.save();

    SonarInstallation i = new SonarGlobalConfiguration().getInstallations()[0];
    String storedConfig = Util.loadFile(new File(Jenkins.getInstance().getRootDir(), d.getId() + ".xml"));

    assertThat(i.getName()).isEqualTo("Name");
    assertThat(i.getServerUrl()).isEqualTo("server.url");
    assertThat(i.getServerAuthenticationToken(any())).isEqualTo(null);
    assertThat(i.getMojoVersion()).isEqualTo("mojoVersion");
    assertThat(i.getAdditionalProperties()).isEqualTo("props");
    assertThat(i.getAdditionalAnalysisProperties()).isEqualTo("key=value");

    assertThat(storedConfig).doesNotContain("dbPasswd");
    assertThat(storedConfig).doesNotContain("sonarPasswd");
  }

  @Test
  public void testAnalysisPropertiesWindows() {
    assertAnalysisPropsWindows("key=value", "/d:key=value");
    assertAnalysisPropsWindows("key=value key2=value2", "/d:key=value", "/d:key2=value2");
    assertAnalysisPropsWindows("-Dkey=value", "/d:-Dkey=value");
    assertAnalysisPropsWindows("");
    assertAnalysisPropsWindows(null);
  }

  @Test
  public void testAnalysisPropertiesUnix() {
    assertAnalysisPropsUnix("key=value", "-Dkey=value");
    assertAnalysisPropsUnix("key=value key2=value2", "-Dkey=value", "-Dkey2=value2");
    assertAnalysisPropsUnix("-Dkey=value", "-D-Dkey=value");
    assertAnalysisPropsUnix("");
    assertAnalysisPropsUnix(null);
  }

  private void assertAnalysisPropsWindows(String input, String... expectedEntries) {
    SonarInstallation inst = new SonarInstallation(null, null, null, null, null, null, input);
    assertThat(inst.getAdditionalAnalysisPropertiesWindows()).isEqualTo(expectedEntries);
  }

  private void assertAnalysisPropsUnix(String input, String... expectedEntries) {
    SonarInstallation inst = new SonarInstallation(null, null, null, null, null, null, input);
    assertThat(inst.getAdditionalAnalysisPropertiesUnix()).isEqualTo(expectedEntries);
  }
}
