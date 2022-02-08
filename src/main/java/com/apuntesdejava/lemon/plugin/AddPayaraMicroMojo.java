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
package com.apuntesdejava.lemon.plugin;

import com.apuntesdejava.lemon.jakarta.jpa.model.ProjectModel;
import com.apuntesdejava.lemon.jakarta.model.types.DatasourceDefinitionStyleType;
import com.apuntesdejava.lemon.plugin.util.PayaraUtil;
import com.apuntesdejava.lemon.plugin.util.ProjectModelUtil;
import com.apuntesdejava.lemon.plugin.util.XmlUtil;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Mojo(name = "add-payara-micro")
public class AddPayaraMicroMojo extends AbstractMojo {

    private static final String PAYARA_MICRO_ID_PROFILE_ELEM = "/project/profiles/profile/id[text()='payara-micro']";
    private static final String COMMAND_LINDE_OPTIONS_ELEM = PAYARA_MICRO_ID_PROFILE_ELEM + "/../build/plugins/plugin/configuration/commandLineOptions";
    private static final String COPY_JDBC_ELEM = PAYARA_MICRO_ID_PROFILE_ELEM + "/../build/plugins/plugin/executions/execution/id[text()='copy-jdbc']";

    @Parameter(
            property = "model",
            defaultValue = "model.json"
    )
    private String _modelProjectFile;
    private ProjectModel projectModel;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Optional<ProjectModel> opt = ProjectModelUtil.getProjectModel(getLog(), _modelProjectFile);
        if (opt.isPresent()) {
            this.projectModel = opt.get();
            addPlugin();
        }
    }

    private void createProfile(Document doc, Element profilesElem) {
        Element payaraMicroProfileElem = doc.createElement("profile");
        profilesElem.appendChild(payaraMicroProfileElem);

        payaraMicroProfileElem.appendChild(XmlUtil.createElement(doc, "id", "payara-micro"));
        payaraMicroProfileElem.appendChild(XmlUtil.createElement(doc, "properties", XmlUtil.createElement(doc, "version.payara", "5.2021.9")));

        payaraMicroProfileElem.appendChild(XmlUtil.createElement(doc, "build",
                XmlUtil.createElement(doc, "plugins",
                        XmlUtil.createElement(doc, "plugin",
                                XmlUtil.createElement(doc, "groupId", "fish.payara.maven.plugins"),
                                XmlUtil.createElement(doc, "artifactId", "payara-micro-maven-plugin"),
                                XmlUtil.createElement(doc, "version", "1.4.0"),
                                XmlUtil.createElement(doc, "configuration",
                                        XmlUtil.createElement(doc, "payaraVersion", "${version.payara}"),
                                        XmlUtil.createElement(doc, "deployWar", "false"),
                                        XmlUtil.createElement(doc, "commandLineOptions",
                                                XmlUtil.createElement(doc, "option",
                                                        XmlUtil.createElement(doc, "key", "--autoBindHttp")
                                                ),
                                                XmlUtil.createElement(doc, "option",
                                                        XmlUtil.createElement(doc, "key", "--deploy"),
                                                        XmlUtil.createElement(doc, "value", "${project.build.directory}/${project.build.finalName}")
                                                )
                                        )
                                )
                        )
                )
        ));
    }

    private void addPlugin() {
        try {
            getLog().debug("Add Payara Micro Plugin");
            Document doc = XmlUtil.getFile(mavenProject.getFile());
            NodeList projectNodeList = XmlUtil.getNodeListByPath(doc, "/project");
            Element projectElem = (Element) projectNodeList.item(0);
            NodeList profilesNodeList = XmlUtil.getNodeListByPath(doc, "/project/profiles");
            Element profilesElem;
            if (profilesNodeList.getLength() == 0) {
                profilesElem = doc.createElement("profiles");
                projectElem.appendChild(profilesElem);
            } else {
                profilesElem = (Element) profilesNodeList.item(0);
            }
            NodeList nodeList = XmlUtil.getNodeListByPath(doc, PAYARA_MICRO_ID_PROFILE_ELEM);
            if (nodeList.getLength() == 0) {
                createProfile(doc, profilesElem);

            }
            DatasourceDefinitionStyleType style = DatasourceDefinitionStyleType.findByValue(projectModel.getDatasource().getStyle());
            if (style == DatasourceDefinitionStyleType.PAYARA_RESOURCES) {
                addPayaraMicroResources(doc);
            }
            if (XmlUtil.getNodeListByPath(doc, COPY_JDBC_ELEM).getLength() == 0) {
                createCopyJdbcExecution(doc);
            }

            XmlUtil.writeXml(doc, mavenProject.getFile());
        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException | TransformerException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void addPayaraMicroResources(Document doc) throws XPathExpressionException {
        Element commandLineOptions = (Element) XmlUtil.getNodeListByPath(doc, COMMAND_LINDE_OPTIONS_ELEM).item(0);
        if (XmlUtil.getNodeListByPath(doc, COMMAND_LINDE_OPTIONS_ELEM + "/option/key[text()='--postbootcommandfile']").getLength() == 0) {
            commandLineOptions.appendChild(
                    XmlUtil.createElement(doc, "option",
                            XmlUtil.createElement(doc, "key", "--postbootcommandfile"),
                            XmlUtil.createElement(doc, "value", "post-boot-commands.txt")
                    )
            );

        }
        if (XmlUtil.getNodeListByPath(doc, COMMAND_LINDE_OPTIONS_ELEM + "/option/key[text()='--addLibs']").getLength() == 0) {
            commandLineOptions.appendChild(
                    XmlUtil.createElement(doc, "option",
                            XmlUtil.createElement(doc, "key", "--addLibs"),
                            XmlUtil.createElement(doc, "value", "target/lib")
                    )
            );
        }
        PayaraUtil.createPayaraMicroDataSourcePostBootFile(getLog(), "post-boot-commands.txt", projectModel, mavenProject);

    }

    private void createCopyJdbcExecution(Document doc) throws XPathExpressionException {
        NodeList pluginsNodeList = XmlUtil.getNodeListByPath(doc, PAYARA_MICRO_ID_PROFILE_ELEM + "/../build/plugins");
        Element pluginsElem = (Element) pluginsNodeList.item(0);
        Element artifactItemsElem;
        pluginsElem.appendChild(
                XmlUtil.createElement(doc, "plugin",
                        XmlUtil.createElement(doc, "groupId", "org.apache.maven.plugins"),
                        XmlUtil.createElement(doc, "artifactId", "maven-dependency-plugin"),
                        XmlUtil.createElement(doc, "executions",
                                XmlUtil.createElement(doc, "execution",
                                        XmlUtil.createElement(doc, "id", "copy-jdbc"),
                                        XmlUtil.createElement(doc, "goals", XmlUtil.createElement(doc, "goal", "copy")),
                                        XmlUtil.createElement(doc, "configuration",
                                                XmlUtil.createElement(doc, "outputDirectory", "target/lib"),
                                                XmlUtil.createElement(doc, "stripVersion", "true"),
                                                artifactItemsElem = XmlUtil.createElement(doc, "artifactItems")
                                        )
                                )
                        )
                )
        );
        Map<String, String> dependencies = (Map<String, String>) projectModel.getDbDefinitions().get("dependency");
        artifactItemsElem.appendChild(
                XmlUtil.createElement(doc, "artifactItem",
                        XmlUtil.createElement(doc, "groupId", dependencies.get("groupId")),
                        XmlUtil.createElement(doc, "artifactId", dependencies.get("artifactId")),
                        XmlUtil.createElement(doc, "version", dependencies.get("version")),
                        XmlUtil.createElement(doc, "type", "jar")
                )
        );
    }

}
