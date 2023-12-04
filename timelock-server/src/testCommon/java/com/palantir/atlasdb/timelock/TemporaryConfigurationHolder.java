/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.timelock;

import com.palantir.test.utils.SubdirectoryCreator;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TemporaryConfigurationHolder implements BeforeAllCallback {

    private static final Configuration TEMPLATE_CONFIG = templateConfig();

    private final File temporaryFolder;
    private final String templateName;
    private final ImmutableTemplateVariables variables;

    private File temporaryConfigFile;

    TemporaryConfigurationHolder(File temporaryFolder, String templateName, TemplateVariables variables) {
        this.temporaryFolder = temporaryFolder;
        this.templateName = templateName;
        this.variables = ImmutableTemplateVariables.copyOf(variables);
    }

    private static Configuration templateConfig() {
        Configuration config = new Configuration(Configuration.VERSION_2_3_29);
        config.setClassLoaderForTemplateLoading(TemporaryConfigurationHolder.class.getClassLoader(), "/");
        config.setDefaultEncoding("UTF-8");
        config.setLocale(Locale.UK);
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return config;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        temporaryConfigFile = Files.createTempFile(temporaryFolder.toPath(), "temporaryConfigFile", null)
                .toFile();
        createTemporaryConfigFile();
    }

    private void createTemporaryConfigFile() throws Exception {
        Template template = TEMPLATE_CONFIG.getTemplate(templateName);
        TemplateVariables variablesWithFolders = variables
                .withDataDirectory(SubdirectoryCreator.createAndGetSubdirectory(temporaryFolder, appendPort("legacy"))
                        .getAbsolutePath())
                .withSqliteDataDirectory(
                        SubdirectoryCreator.createAndGetSubdirectory(temporaryFolder, appendPort("sqlite"))
                                .getAbsolutePath());
        template.process(
                variablesWithFolders, Files.newBufferedWriter(temporaryConfigFile.toPath(), StandardCharsets.UTF_8));
    }

    private String appendPort(String legacy) {
        return legacy + variables.getLocalServerPort();
    }

    String getTemporaryConfigFileLocation() {
        return temporaryConfigFile.getPath();
    }
}
