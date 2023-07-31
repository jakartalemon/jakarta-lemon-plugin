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
 * Utility class for handling the pom.xml file
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class ProjectModelUtil {

    private static ThreadLocal<JsonObject> DEPENDENCIES_DEFINITIONS = null;

    private ProjectModelUtil() {

    }

    /**
     * Gets the properties of a given Maven profile
     *
     * @param profile Maven Profile
     * @return Properties
     */
    public static Properties getProperties(Profile profile) {
        return Optional.ofNullable(profile.getProperties()).orElseGet(() -> {
            Properties props = new Properties();
            profile.setProperties(props);
            return profile.getProperties();
        });
    }

    /**
     * Gets the configuration of a plugin in XML format
     *
     * @param plugin The plugin where the configuration will be obtained from
     * @return The plugin configuration in XML format {@link Xpp3Dom} , manipulable by Maven
     */
    public static Xpp3Dom getConfiguration(ConfigurationContainer plugin) {
        return Optional.ofNullable((Xpp3Dom) plugin.getConfiguration()).orElseGet(() -> {
            Xpp3Dom xpp3Dom = new Xpp3Dom(CONFIGURATION);
            plugin.setConfiguration(xpp3Dom);
            return xpp3Dom;
        });
    }

    /**
     * Gets the {@code  model.json } file that contains the configuration of the model to be created in the project.
     *
     * @param log              Maven log
     * @param modelProjectFile Model File
     * @return Model Configuration Json Object, or empty if not found
     */
    public static Optional<JsonObject> getProjectModel(Log log, String modelProjectFile) {
        log.debug("Reading model configuration:" + modelProjectFile);
        try (InputStream in = new FileInputStream(modelProjectFile)) {
            return Optional.ofNullable(Json.createReader(in).readObject());
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        log.error("Model configuration file :" + modelProjectFile + " not found");
        return Optional.empty();
    }

    /**
     * Gets the given Maven project profile, and if it doesn't exist, a new one will be created.
     *
     * @param model       Maven model
     * @param profileName Profile Name
     * @return Profile object
     */
    public static Profile getProfile(Model model, String profileName) {
        return model.getProfiles().stream().filter(p -> p.getId().equals(profileName)).findFirst().orElseGet(() -> {
            Profile profile = new Profile();
            profile.setId(profileName);
            model.addProfile(profile);
            return profile;
        });
    }

    /**
     * Gets the Plugin Manager given a BuildBase. If it does not exist, it is created.
     *
     * @param buildBase the value of buildBase
     * @return PluginManagement
     */
    public static PluginManagement getPluginManagement(BuildBase buildBase) {
        return Optional.ofNullable(buildBase.getPluginManagement()).orElseGet(() -> {
            PluginManagement pluginManagement = new PluginManagement();
            buildBase.setPluginManagement(pluginManagement);
            return pluginManagement;
        });
    }

    /**
     * Inserts a plugin, based on groupId and artifactId, to the Plugin container. Returns the inserted object.
     *
     * @param pluginContainer Plugin container
     * @param groupId         Group ID
     * @param artifactId      Artifact ID
     * @return Plugin Object, Empty if not inserted.
     */
    public static Optional<Plugin> addPlugin(PluginContainer pluginContainer, String groupId, String artifactId) {
        return addPlugin(pluginContainer, groupId, artifactId, StringUtils.EMPTY);
    }

    /**
     * Add a plugin to the container, given a group ID, artifact ID, and a specific version
     *
     * @param pluginContainer Plugin container
     * @param groupId         Group ID
     * @param artifactId      Artifact ID
     * @param version         Version
     * @return Plugin Object, Empty if not inserted.
     */
    public static Optional<Plugin> addPlugin(PluginContainer pluginContainer,
                                             String groupId,
                                             String artifactId,
                                             String version) {
        return addPlugin(pluginContainer, groupId, artifactId, version, null);
    }

    /**
     * Adds a plugin to the container, given a group ID, artifact ID, a specific version, and a map of additional
     * settings.
     *
     * @param pluginContainer      Plug container
     * @param groupId              Group ID
     * @param artifactId           Artifact ID
     * @param version              Version
     * @param configurationOptions map of additional settings
     * @return Plugin created
     */
    public static Optional<Plugin> addPlugin(PluginContainer pluginContainer,
                                             String groupId,
                                             String artifactId,
                                             String version,
                                             Map<String, ?> configurationOptions) {
        List<Plugin> plugins = pluginContainer.getPlugins();
        return plugins.stream()
            .filter(item -> item.getGroupId().equals(groupId) && item.getArtifactId().equals(artifactId))
            .findFirst()
            .or(() -> {
                Plugin plugin = new Plugin();
                plugin.setGroupId(groupId);
                plugin.setArtifactId(artifactId);
                if (StringUtils.isNotBlank(version)) {
                    plugin.setVersion(version);
                }
                setConfigurationOptions(plugin, configurationOptions);

                pluginContainer.addPlugin(plugin);
                return Optional.of(plugin);
            });

    }

    /**
     * Sets the configuration options for a plugin
     *
     * @param plugin               plugin
     * @param configurationOptions configuration options
     */
    public static void setConfigurationOptions(ConfigurationContainer plugin, Map<String, ?> configurationOptions) {
        Optional.ofNullable(configurationOptions).ifPresent(conf -> {
            if (!conf.isEmpty()) {
                var xpp3DomConf = getConfiguration(plugin);

                conf.forEach((name, value) -> {
                    if (value instanceof String) {
                        addChildren(xpp3DomConf, name, true).setValue(value.toString());
                    } else if (value instanceof List) {
                        addConfiguration(xpp3DomConf, name, (List<?>) value);
                    } else if (value instanceof Map) {
                        addConfiguration(xpp3DomConf, name, (Map<String, ?>) value);
                    }
                });
                plugin.setConfiguration(xpp3DomConf);
            }
        });
    }

    private static void addConfiguration(Xpp3Dom parent, String elementName, Map<String, ?> configuration) {
        Xpp3Dom newChildDom = addChildren(parent, elementName, true);
        configuration.forEach((name, value) -> addConfigurationIteration(newChildDom, name, value));
    }

    private static void addConfiguration(Xpp3Dom parent, String elementName, List<?> configuration) {
        Xpp3Dom newChildDom = addChildren(parent, elementName, true);
        configuration.forEach(value -> addConfigurationIteration(newChildDom, OPTION, value));
    }

    private static void addConfigurationIteration(Xpp3Dom newChildDom, String name, Object value) {
        if (value instanceof String) {
            addChildren(newChildDom, name, true).setValue((String) value);

        } else if (value instanceof Map) {
            addConfiguration(newChildDom, name, (Map<String, ?>) value);
        } else if (value instanceof List) {
            addConfiguration(newChildDom, name, (List<?>) value);

        }
    }

    /**
     * Get build base from Profile
     *
     * @param profile Profile
     * @return Buildbase
     */
    public static BuildBase getBuildBase(Profile profile) {
        return Optional.ofNullable(profile.getBuild()).orElseGet(() -> {
            BuildBase b = new BuildBase();
            profile.setBuild(b);
            return b;
        });
    }

    /**
     * Save the XML model of the maven project
     *
     * @param mavenProject Maven Project
     * @param model        Maven Project to save
     * @throws IOException IO Exception
     */
    public static void saveModel(MavenProject mavenProject, Model model) throws IOException {
        File projectFile = mavenProject.getFile();
        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(new FileWriter(projectFile), model);
    }

    /**
     * Gets the XML model of the maven project
     *
     * @param mavenProject Maven Project
     * @return Maven Project to handle
     * @throws IOException            IOException
     * @throws XmlPullParserException XmlPullParserException
     */
    public static Model getModel(MavenProject mavenProject) throws IOException, XmlPullParserException {
        File projectFile = mavenProject.getFile();
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(new FileReader(projectFile));
    }

    /**
     * Adds an element with the given name. Returns the created XML element {@link Xpp3Dom} .
     *
     * @param parent Parent element
     * @param name   Child element name
     * @return Child Xpp3Dom created
     */
    public static Xpp3Dom addChildren(Xpp3Dom parent, String name) {
        return addChildren(parent, name, false);
    }

    /**
     * Adds an element with the given name.Returns the created XML element {@link Xpp3Dom} .
     *
     * @param parent          Parent element
     * @param name            Child element name
     * @param ignoreDuplicate {@code  true } if it ignores repeating elements
     * @return Child Xpp3Dom created
     */
    public static Xpp3Dom addChildren(Xpp3Dom parent, String name, boolean ignoreDuplicate) {
        return ignoreDuplicate
            ? createChildren(parent, name)
            : Arrays.stream(parent.getChildren())
                .filter(item -> item.getName().equals(name))
                .findFirst()
                .orElseGet(() -> createChildren(parent, name));
    }

    private static Xpp3Dom createChildren(Xpp3Dom parent, String name) {
        Xpp3Dom xpp3Dom = new Xpp3Dom(name);
        parent.addChild(xpp3Dom);
        return xpp3Dom;
    }

    /**
     * Adds a dependency based on the database name, to the dependency group.
     *
     * @param log             Maven log
     * @param dependencyGroup Dependency Group
     * @param database        Database Name, may be: mysql, postgresql, etc.
     */
    public static void addDependenciesDatabase(Log log, Xpp3Dom dependencyGroup, String database) {
        DependenciesUtil.getByDatabase(log, database).ifPresent(dependen -> {
            Xpp3Dom dependency = ProjectModelUtil.addChildren(dependencyGroup, DEPENDENCY);
            addChildren(dependency, DEPENDENCY_GROUP_ID).setValue(dependen.getString(DEPENDENCY_GROUP_ID));
            addChildren(dependency, DEPENDENCY_ARTIFACT_ID).setValue(dependen.getString(DEPENDENCY_ARTIFACT_ID));
            addChildren(dependency, DEPENDENCY_VERSION).setValue(dependen.getString(DEPENDENCY_VERSION));
        });

    }

    /**
     * Adds a dependency based on the database name, to the Project Model dependency group.
     * @param log Maven log
     * @param model Project Model
     * @param database Databas name
     */
    public static void addDependenciesDatabase(Log log, Model model, String database) {
        addDependency(DependenciesUtil.getByDatabase(log, database).orElse(null), model.getDependencies(), emptyMap());

    }

    private static Dependency addDependency(JsonObject dependencyJson,
                                            List<Dependency> dependencies,
                                            Map<String, String> props) {
        return dependencies.stream()
            .filter(
                item -> item.getGroupId().equals(dependencyJson.getString(DEPENDENCY_GROUP_ID)) && item.getArtifactId()
                    .equals(dependencyJson.getString(DEPENDENCY_ARTIFACT_ID)))
            .findFirst()
            .orElseGet(() -> {
                Dependency dependency = new Dependency();
                dependency.setGroupId(dependencyJson.getString(DEPENDENCY_GROUP_ID));
                dependency.setArtifactId(dependencyJson.getString(DEPENDENCY_ARTIFACT_ID));
                dependency.setVersion(dependencyJson.getString(DEPENDENCY_VERSION));
                if (props.containsKey(CLASSIFIER)) {
                    dependency.setClassifier(props.get(CLASSIFIER));
                }
                if (props.containsKey(SCOPE)) {
                    dependency.setScope(props.get(SCOPE));
                }
                dependencies.add(dependency);
                return dependency;
            });
    }

    /**
     * Adds a dependency to the list, given by the group ID and artifact ID.
     *
     * @param log          Maven log
     * @param dependencies dependencies list
     * @param groupId      Group ID
     * @param artefactId   Artefact ID
     * @return Dependency created
     */
    public static Dependency addDependency(Log log, List<Dependency> dependencies, String groupId, String artefactId) {
        return addDependency(log, dependencies, groupId, artefactId, emptyMap());
    }

    /**
     * Adds a dependency to the list, given by the group ID, artifact ID, and properties.
     *
     * @param log          Maven log
     * @param dependencies dependencies list
     * @param groupId      Group ID
     * @param artefactId   Artefact ID
     * @param props        Dependency properties
     * @return Dependency created
     */
    public static Dependency addDependency(Log log,
                                           List<Dependency> dependencies,
                                           String groupId,
                                           String artefactId,
                                           Map<String, String> props) {
        return addDependency(
            DependenciesUtil.getLastVersionDependency(log, String.format("g:%s+AND+a:%s", groupId, artefactId))
                .orElse(null), dependencies, props);
    }

    /**
     * Gets the driver of a database name. The definitions are taken from the project website.
     *
     * @param log    Maven log
     * @param dbName Database name
     * @return JDBC Driver
     * @throws IOException          IOException
     * @throws InterruptedException InterruptedException
     * @throws URISyntaxException   URISyntaxException
     */
    public static String getDriver(Log log, String dbName) throws IOException, InterruptedException,
                                                                  URISyntaxException {
        if (DEPENDENCIES_DEFINITIONS == null) {
            DEPENDENCIES_DEFINITIONS = ThreadLocal.withInitial(() -> {
                try {
                    return HttpClientUtil.getJson(log, DEPENDENCIES_URL, JsonReader::readObject);
                } catch (IOException | InterruptedException | URISyntaxException ex) {
                    log.error(ex.getMessage(), ex);
                }
                return null;
            });
        }
        var dependenciesDefinitions = DEPENDENCIES_DEFINITIONS.get();
        return dependenciesDefinitions.getJsonObject(dbName).getString(DATASOURCE);
    }

}
