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

import com.apuntesdejava.lemon.jakarta.jpa.model.ProjectModel;
import com.apuntesdejava.lemon.jakarta.model.DependencyModel;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.util.*;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class ProjectModelUtil {

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

    public static Optional<ProjectModel> getProjectModel(Log log, String modelProjectFile) {
        log.debug("Reading model configuration:" + modelProjectFile);
        try ( InputStream in = new FileInputStream(modelProjectFile)) {
            Jsonb jsonb = JsonbBuilder.create();
            return Optional.ofNullable(jsonb.fromJson(in, ProjectModel.class));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        log.error("Model configuration file :" + modelProjectFile + " not found");
        return Optional.empty();
    }

    public static Profile getProfile(Model model, String profile) {
        return model.getProfiles().stream().filter(p -> p.getId().equals(profile)).findFirst().orElseGet(() -> {
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

    public static Optional<Plugin> addPlugin(PluginContainer pluginContainer, String groupId, String artifactId) {
        return addPlugin(pluginContainer, groupId, artifactId, null);
    }

    public static Optional<Plugin> addPlugin(PluginContainer pluginContainer, String groupId, String artifactId, String version) {
        List<Plugin> plugins = pluginContainer.getPlugins();
        Optional<Plugin> plugin = plugins.stream().filter(item -> item.getGroupId().equals(groupId) && item.getArtifactId().equals(artifactId)).findFirst();
        if (plugin.isEmpty()) {
            Plugin p = new Plugin();
            p.setGroupId(groupId);
            p.setArtifactId(artifactId);
            if (StringUtils.isNotBlank(version)) {
                p.setVersion(version);
            }
            pluginContainer.addPlugin(p);
            return Optional.of(p);
        }
        return plugin;
    }

    public static BuildBase getBuild(Profile profile) {
        return Optional.ofNullable(profile.getBuild()).orElseGet(() -> {
            BuildBase b = new BuildBase();
            profile.setBuild(b);
            return b;
        });
    }

    public static void saveModel(MavenProject mavenProject, Model model) throws IOException {
        File projectFile = mavenProject.getFile();
        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(new FileWriter(projectFile), model);
    }

    public static Model getModel(MavenProject mavenProject) throws IOException, XmlPullParserException {
        File projectFile = mavenProject.getFile();
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(new FileReader(projectFile));
    }

    public static Xpp3Dom addChildren(Xpp3Dom parent, String name) {
        return Arrays.stream(parent.getChildren())
                .filter(item -> item.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    Xpp3Dom xpp3Dom = new Xpp3Dom(name);
                    parent.addChild(xpp3Dom);
                    return xpp3Dom;
                });
    }

    public static void addDependenciesDatabase(Xpp3Dom dependency, String database) {

        DependencyModel dependen = DependenciesUtil.getByDatabase(database);

        ProjectModelUtil.addChildren(dependency, "groupId").setValue((String) dependen.getGroupId());
        ProjectModelUtil.addChildren(dependency, "artifactId").setValue((String) dependen.getArtifactId());
        ProjectModelUtil.addChildren(dependency, "version").setValue((String) dependen.getVersion());

    }

    private ProjectModelUtil() {

    }

}
