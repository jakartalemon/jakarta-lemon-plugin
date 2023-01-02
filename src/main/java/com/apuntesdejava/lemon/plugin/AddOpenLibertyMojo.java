/*
 * Copyright 2022 Apuntes de Java.
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
package com.apuntesdejava.lemon.plugin;

import com.apuntesdejava.lemon.plugin.util.HttpClientUtil;
import com.apuntesdejava.lemon.plugin.util.OpenLibertyUtil;
import com.apuntesdejava.lemon.plugin.util.ProjectModelUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.xml.bind.JAXBException;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Mojo(name = "add-openliberty")
public class AddOpenLibertyMojo extends AbstractMojo {

    @Parameter(
        property = "model",
        defaultValue = "model.json"
    )
    private String _modelProjectFile;
    @Parameter(
        defaultValue = "${project}",
        readonly = true
    )
    private MavenProject mavenProject;

    @Parameter(
        defaultValue = "9080",
        property = "system.http.port"
    )
    private String systemHttpPort;

    @Parameter(
        defaultValue = "9080",
        property = "default.http.port"
    )
    private String defaultHttpPort;

    @Parameter(
        defaultValue = "9443",
        property = "default.https.port"
    )
    private String defaultHttpsPort;

    private JsonObject projectModel;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ProjectModelUtil.getProjectModel(getLog(), _modelProjectFile).ifPresent(pm -> {
            this.projectModel = pm;
            addPlugin();
            createServerXml();
        });

    }

    private void addPlugin() {
        try {
            getLog().debug("Add OpenLiberty Plugin");
            var appName = mavenProject.getName();
            Model model = ProjectModelUtil.getModel(mavenProject);
            Profile profile = ProjectModelUtil.getProfile(model, OPENLIBERTY);
            Properties props = ProjectModelUtil.getProperties(profile);
            props.setProperty(LIBERTY_VAR_SYSTEM_HTTP_PORT, systemHttpPort);
            props.setProperty(LIBERTY_VAR_DEFAULT_HTTP_PORT, defaultHttpPort);
            props.setProperty(LIBERTY_VAR_DEFAULT_HTTPS_PORT, defaultHttpsPort);
            props.setProperty(LIBERTY_VAR_APP_CONTEXT_ROOT, appName);
            var build = ProjectModelUtil.getBuild(profile);
            var pm = ProjectModelUtil.getPluginManagement(build);
            try {
                var config = HttpClientUtil.getJson(getLog(), LEMON_CONFIG_URL, JsonReader::readObject);
                var pluginInfo = config.getJsonObject(OPENLIBERTY)
                    .getJsonObject(PLUGIN);
                Map<String, Object> configOptions = new LinkedHashMap<>(Map.of(SERVER_NAME, appName));
                if (pluginInfo.getJsonObject(CONFIGURATION).getJsonObject(RUNTIME_ARTIFACT).getBoolean(ENABLED)) {
                    var runtimeArtifact = pluginInfo.getJsonObject(CONFIGURATION).getJsonObject(RUNTIME_ARTIFACT);
                    configOptions.put(RUNTIME_ARTIFACT, Map.of(
                        DEPENDENCY_GROUP_ID, runtimeArtifact.getString(DEPENDENCY_GROUP_ID),
                        DEPENDENCY_ARTIFACT_ID, runtimeArtifact.getString(DEPENDENCY_ARTIFACT_ID),
                        DEPENDENCY_VERSION, runtimeArtifact.getString(DEPENDENCY_VERSION),
                        DEPENDENCY_TYPE, runtimeArtifact.getString(DEPENDENCY_TYPE)
                    ));
                }
                ProjectModelUtil.addPlugin(pm, "io.openliberty.tools", "liberty-maven-plugin", pluginInfo.getString(DEPENDENCY_VERSION), configOptions);
            } catch (InterruptedException | URISyntaxException ex) {
                getLog().error(ex.getMessage(), ex);
            }
            ProjectModelUtil.addPlugin(pm, MAVEN_PLUGIN_GROUP_ID, "maven-war-plugin", "3.3.2");
            ProjectModelUtil.addPlugin(build, MAVEN_PLUGIN_GROUP_ID, "maven-failsafe-plugin", "2.22.2",
                Map.of(SYSTEM_PROPERTY_VARIABLES,
                    Map.of("http.port", String.format("${%s}", LIBERTY_VAR_DEFAULT_HTTP_PORT))
                ));
            ProjectModelUtil.saveModel(mavenProject, model);

        } catch (IOException | XmlPullParserException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createServerXml() {
        try {
            OpenLibertyUtil.getServerModel(getLog(), mavenProject, Map.of(
                    LIBERTY_VAR_SYSTEM_HTTP_PORT, systemHttpPort,
                    LIBERTY_VAR_DEFAULT_HTTP_PORT, defaultHttpPort,
                    LIBERTY_VAR_DEFAULT_HTTPS_PORT, defaultHttpsPort
                ))
                .ifPresent(serverModel -> {
                    try {
                        OpenLibertyUtil.saveServerModel(mavenProject, serverModel);
                    } catch (JAXBException ex) {
                        getLog().error(ex.getMessage(), ex);
                    }
                });

        } catch (IOException | JAXBException | ParserConfigurationException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }
}
