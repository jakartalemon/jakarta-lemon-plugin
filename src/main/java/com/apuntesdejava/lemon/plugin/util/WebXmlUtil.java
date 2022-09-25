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
package com.apuntesdejava.lemon.plugin.util;

import com.apuntesdejava.lemon.jakarta.webxml.model.SessionConfigModel;
import com.apuntesdejava.lemon.jakarta.webxml.model.WebAppModel;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class WebXmlUtil extends AbstractXmlUtil<WebAppModel> {


    public WebXmlUtil(String basedir) {
        super(WebAppModel.class, basedir);
    }

    @Override
    protected WebAppModel newModelInstance() {
        var model = new WebAppModel();
        model.setSessionConfig(new SessionConfigModel("30"));
        return model;
    }

    @Override
    protected Path getXmlFullPath(String baseDir) {
        return Paths.get(baseDir, "src", "main", "webapp", "WEB-INF", "web.xml").normalize();
    }

    @Override
    public void saveModel(WebAppModel model) throws JAXBException {
        super.saveModel(model, (marshaller) -> {
            try {
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd");
            } catch (PropertyException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
