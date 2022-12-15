/*
 * Copyright 2021 Apuntes de Java.
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
package com.apuntesdejava.lemon.plugin.util;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;
import static java.util.Collections.emptyMap;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class ProjectModelUtil {

    private static ThreadLocal<JsonObject> DEPENDENCIES_DEFINITIONS = null;

    private ProjectModelUtil() {

    }

    public static Properties getProperties(Profile profile) {
        return Optional.ofNullable(profile.getProperties())
                .orElseGet(() -> {
                    Properties props = new Properties();
                    profile.setProperties(props);
                    return profile.getProperties();
                });
    }

    public static Xpp3Dom getConfiguration(ConfigurationContainer plugin) {
        return Optional.ofNullable((Xpp3Dom) plugin.getConfiguration())
                .orElseGet(() -> {
                    Xpp3Dom xpp3Dom = new Xpp3Dom("configuration");
                    plugin.setConfiguration(xpp3Dom);
                    return xpp3Dom;
                });
    }

    public static Optional<JsonObject> getProjectModel(Log log,
                                                       String modelProjectFile) {
        log.debug("Reading model configuration:" + modelProjectFile);
        try (InputStream in = new FileInputStream(modelProjectFile)) {
            return Optional.ofNullable(Json.createReader(in).readObject());
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        log.error("Model configuration file :" + modelProjectFile + " not found");
        return Optional.empty();
    }

    public static Profile getProfile(Model model,
                                     String profile) {
        return model.getProfiles().stream().filter(p -> p.getId().equals(profile)).findFirst()
                .orElseGet(() -> {
                    Profile p = new Profile();
                    p.setId(profile);
                    model.addProfile(p);
                    return p;
                });
    }

    public static PluginManagement getPluginManagement(BuildBase build) {
        return Optional.ofNullable(build.getPluginManagement()).orElseGet(() -> {
            PluginManagement $pm = new PluginManagement();
            build.setPluginManagement($pm);
            return $pm;
        });
    }

    public static Optional<Plugin> addPlugin(
            PluginContainer pluginContainer,
            String groupId,
            String artifactId
    ) {
        return addPlugin(pluginContainer, groupId, artifactId, null);
    }

    public static Optional<Plugin> addPlugin(
            PluginContainer pluginContainer,
            String groupId,
            String artifactId,
            String version
    ) {
        List<Plugin> plugins = pluginContainer.getPlugins();
        return plugins.stream()
                .filter(item -> item.getGroupId().equals(groupId)
                        && item.getArtifactId().equals(artifactId)).findFirst().or(() -> {
                    Plugin p = new Plugin();
                    p.setGroupId(groupId);
                    p.setArtifactId(artifactId);
                    if (StringUtils.isNotBlank(version)) {
                        p.setVersion(version);
                    }
                    pluginContainer.addPlugin(p);
                    return Optional.of(p);
                });

    }

    public static BuildBase getBuild(Profile profile) {
        return Optional.ofNullable(profile.getBuild()).orElseGet(() -> {
            BuildBase b = new BuildBase();
            profile.setBuild(b);
            return b;
        });
    }

    public static void saveModel(MavenProject mavenProject,
                                 Model model) throws IOException {
        File projectFile = mavenProject.getFile();
        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(new FileWriter(projectFile), model);
    }

    public static Model getModel(MavenProject mavenProject)
            throws IOException, XmlPullParserException {
        File projectFile = mavenProject.getFile();
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(new FileReader(projectFile));
    }

    public static Xpp3Dom addChildren(Xpp3Dom parent,
                                      String name) {
        return addChildren(parent, name, false);
    }

    public static Xpp3Dom addChildren(Xpp3Dom parent,
                                      String name,
                                      boolean ignoreDuplicate) {
        return ignoreDuplicate
                ? createChildren(parent, name)
                : Arrays.stream(parent.getChildren())
                        .filter(item -> item.getName().equals(name))
                        .findFirst()
                        .orElseGet(() -> createChildren(parent, name));
    }

    private static Xpp3Dom createChildren(Xpp3Dom parent,
                                          String name) {
        Xpp3Dom xpp3Dom = new Xpp3Dom(name);
        parent.addChild(xpp3Dom);
        return xpp3Dom;
    }

    public static void addDependenciesDatabase(Log log,
                                               Xpp3Dom dependency,
                                               String database) {

        DependenciesUtil.getByDatabase(log, database).ifPresent(dependen -> {

            ProjectModelUtil.addChildren(dependency, DEPENDENCY_GROUP_ID).setValue(dependen.getString(
                    DEPENDENCY_GROUP_ID));
            ProjectModelUtil.addChildren(dependency, DEPENDENCY_ARTIFACT_ID)
                    .setValue(dependen.getString(DEPENDENCY_ARTIFACT_ID));
            ProjectModelUtil.addChildren(dependency, DEPENDENCY_VERSION)
                    .setValue(dependen.getString(DEPENDENCY_VERSION));
        });

    }

    public static Dependency addDependenciesDatabase(Log log,
                                                     Model model,
                                                     String database) {
        return addDependency(DependenciesUtil.getByDatabase(log, database).orElse(null),
                             model.getDependencies(),
                             emptyMap());

    }

    private static Dependency addDependency(
            JsonObject dependencyJson,
            List<Dependency> dependencies,
            Map<String, String> props
    ) {
        return dependencies.stream()
                .filter(item
                                -> item.getGroupId().equals(dependencyJson.getString(DEPENDENCY_GROUP_ID))
                        && item.getArtifactId().equals(dependencyJson.getString(DEPENDENCY_ARTIFACT_ID))
                ).findFirst()
                .orElseGet(() -> {
                    Dependency dependency = new Dependency();
                    dependency.setGroupId(dependencyJson.getString(DEPENDENCY_GROUP_ID));
                    dependency.setArtifactId(dependencyJson.getString(DEPENDENCY_ARTIFACT_ID));
                    dependency.setVersion(dependencyJson.getString(DEPENDENCY_VERSION));
                    if (props.containsKey("classifier")) {
                        dependency.setClassifier(props.get("classifier"));
                    }
                    if (props.containsKey("scope")) {
                        dependency.setScope(props.get("scope"));
                    }
                    dependencies.add(dependency);
                    return dependency;
                });
    }

    public static Dependency addDependency(
            Log log,
            Model model,
            String groupId,
            String artefactId
    ) {
        return addDependency(log, model, groupId, artefactId, emptyMap());
    }

    public static Dependency addDependency(
            Log log,
            Model model,
            String groupId,
            String artefactId,
            Map<String, String> props
    ) {
        return addDependency(DependenciesUtil
                                     .getLastVersionDependency(log,
                                                               String.format("g:%s+AND+a:%s", groupId,
                                                                             artefactId))
                                     .orElse(null),
                             model.getDependencies(),
                             props
        );
    }

    public static String getDriver(Log log,
                                   String dbName)
            throws IOException, InterruptedException, URISyntaxException {
        if (DEPENDENCIES_DEFINITIONS == null) {
            DEPENDENCIES_DEFINITIONS = new ThreadLocal<>() {
                @Override
                protected JsonObject initialValue() {
                    try {
                        return HttpClientUtil.getJson(log, DEPENDENCIES_URL,
                                                      JsonReader::readObject);
                    } catch (IOException | InterruptedException | URISyntaxException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                    return null;
                }
            };
        }
        var dependenciesDefinitions = DEPENDENCIES_DEFINITIONS.get();
        return dependenciesDefinitions.getJsonObject(dbName).getString(DATASOURCE);
    }

}
