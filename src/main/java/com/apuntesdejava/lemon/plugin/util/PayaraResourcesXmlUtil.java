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

import com.apuntesdejava.lemon.jakarta.payararesources.model.PayaraResourcesModel;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class PayaraResourcesXmlUtil extends AbstractXmlUtil<PayaraResourcesModel> {

    private static final String PAYARA_RESOURCE_DOCTYPE_PUBLIC = "-//Payara.fish//DTD Payara Server 4 Resource Definitions//EN";
    private static final String PAYARA_RESOURCE_DOCTYPE_SYSTEM = "https://raw.githubusercontent.com/payara/Payara-Community-Documentation/master/docs/modules/ROOT/pages/schemas/payara-resources_1_6.dtd";

    public PayaraResourcesXmlUtil(String basedir) {
        super(PayaraResourcesModel.class, basedir);
    }

    @Override
    protected PayaraResourcesModel newModelInstance() {
        return new PayaraResourcesModel();
    }

    @Override
    protected Path getXmlFullPath(String baseDir) {
        return Paths.get(baseDir, "src", "main", "setup", "payara-resources.xml").normalize();
    }
}
