package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;
import com.tabulify.exception.CastException;
import com.tabulify.exception.NoManifestException;
import com.tabulify.java.JarManifest;
import com.tabulify.java.JarManifestAttribute;
import com.tabulify.glob.Glob;
import com.tabulify.type.Casts;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class TabulVersion {

    public static final String ATTRIBUTE_SELECTORS = "attribute-selectors";

    public static final Set<JarManifestAttribute> excluded = Set.of(
            /**
             * Manifest version (1.0) has no utility
             */
            JarManifestAttribute.MANIFEST_VERSION,
            /**
             * Created by (ie Maven JAR Plugin 3.4.1)
             */
            JarManifestAttribute.CREATED_BY,
            /**
             * Message full does not pass in a tabular format
             */
            JarManifestAttribute.GIT_COMMIT_MESSAGE_FULL,
            /**
             * No personal info
             * (Commit are done now under gmail and build host may be the home computer)
             */
            JarManifestAttribute.BUILD_HOST,
            /**
             * This is not an API and an implementation
             */
            JarManifestAttribute.IMPLEMENTATION_TITLE,
            JarManifestAttribute.IMPLEMENTATION_VERSION,
            JarManifestAttribute.IMPLEMENTATION_VENDOR,
            JarManifestAttribute.SPECIFICATION_TITLE,
            JarManifestAttribute.SPECIFICATION_VERSION,
            JarManifestAttribute.SPECIFICATION_VENDOR
    );

    public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

        // Command
        childCommand
                .setDescription(
                        "Print the version and build information"
                )
                .addExample(
                        "Show all version and build information",
                        CliUsage.CODE_BLOCK,
                        CliUsage.getFullChainOfCommand(childCommand) + " *",
                        CliUsage.CODE_BLOCK
                )
                .addExample(
                        "Show only the version properties",
                        CliUsage.CODE_BLOCK,
                        CliUsage.getFullChainOfCommand(childCommand) + " *version*",
                        CliUsage.CODE_BLOCK
                );

        childCommand.addArg(ATTRIBUTE_SELECTORS)
                .setDescription("One or more name attribute selectors (ie pattern)")
                .setDefaultValue("*");

        // Arguments
        final CliParser cliParser = childCommand.parse();


        final List<Glob> attributeSelectors = cliParser
                .getStrings(ATTRIBUTE_SELECTORS)
                .stream()
                .map(Glob::createOf)
                .collect(Collectors.toList());

        DataPath tabularVersion = tabular.getAndCreateRandomMemoryDataPath()
                .setLogicalName("Version")
                .getOrCreateRelationDef()
                .addColumn("Name")
                .addColumn("Value")
                .addColumn("Description")
                .getDataPath();

        JarManifest jarManifest;
        try {
            jarManifest = JarManifest.createFor(Tabul.class);
        } catch (NoManifestException e) {
            try (InsertStream inputStream = tabularVersion.getInsertStream()) {
                inputStream.insert(
                        "no-manifest",
                        "true",
                        "No manifest was found"
                );
            }
            return Collections.singletonList(tabularVersion);
        }

        try (InsertStream inputStream = tabularVersion.getInsertStream()) {
            for (String key : jarManifest.getMap().keySet().stream().sorted().collect(Collectors.toList())) {
                boolean match = false;
                for (Glob attributeSelector : attributeSelectors) {
                    if (attributeSelector.matches(key)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
                String description = "";
                JarManifestAttribute manifestAttribute = null;
                try {
                    manifestAttribute = Casts.cast(key, JarManifestAttribute.class);
                } catch (CastException e) {
                    //
                }
                String value = jarManifest.getAttributeValue(key);
                if (manifestAttribute != null) {
                    if (excluded.contains(manifestAttribute)) {
                        continue;
                    }
                    description = manifestAttribute.getDescription();
                    if (manifestAttribute == JarManifestAttribute.GIT_COMMIT_USER_EMAIL && value.equals("gerardnico@gmail.com")) {
                        value = "nico@tabulify.com";
                    }
                }
                inputStream.insert(
                        tabular.toPublicName(key),
                        value,
                        description
                );
            }
        }

        return Collections.singletonList(tabularVersion);

    }


}
