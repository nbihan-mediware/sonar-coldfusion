package com.stepstone.sonar.plugin.coldfusion;

import com.stepstone.sonar.plugin.coldfusion.profile.ColdFusionProfileExporter;
import com.stepstone.sonar.plugin.coldfusion.profile.ColdFusionSonarWayProfileImporter;
import com.stepstone.sonar.plugin.coldfusion.rules.ColdFusionCommonRulesDecorator;
import com.stepstone.sonar.plugin.coldfusion.rules.ColdFusionCommonRulesEngine;
import com.stepstone.sonar.plugin.coldfusion.rules.ColdFusionSonarRulesDefinition;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

@Properties({
        @Property(
                key = ColdFusionPlugin.FILE_SUFFIXES_KEY,
                defaultValue = ColdFusionPlugin.FILE_SUFFIXES_DEFVALUE,
                name = "File suffixes",
                description = "Comma-separated list of suffixes of files to analyze.",
                project = true,
                global = true
        ),
        @Property(
                key = ColdFusionPlugin.CFLINT_JAR_PATH,
                defaultValue = "CFLint-all.jar",
                name = "CFlint jar",
                description = "Absolute path to CFLint jar file",
                project = true,
                global = true
        ),
        @Property(
                key = ColdFusionPlugin.CFLINT_JAVA,
                defaultValue = "java",
                name = "Java executable",
                description = "",
                project = true,
                global = true
        ),
        @Property(
                key = ColdFusionPlugin.CFLINT_JAVA_OPTS,
                defaultValue = "",
                name = "Java executable options",
                description = "Additional parameters passed to java process. E.g. -Xmx1g",
                project = true,
                global = true
        ),
})
public class ColdFusionPlugin extends SonarPlugin {

    public static final String LANGUAGE_KEY = "cf";
    public static final String LANGUAGE_NAME = "ColdFusion";

    public static final String FILE_SUFFIXES_KEY = "sonar.cf.file.suffixes";
    public static final String FILE_SUFFIXES_DEFVALUE = ".cfc,.cfm";

    public static final String REPOSITORY_KEY = "coldfusionsquid";
    public static final String REPOSITORY_NAME = "SonarQube";

    public static final String CFLINT_JAR_PATH = "sonar.cf.cflint.jar.path";
    public static final String CFLINT_JAVA = "sonar.cf.cflint.java";
    public static final String CFLINT_JAVA_OPTS = "sonar.cf.cflint.java.opts";

    @Override
    public List getExtensions() {
        return Arrays.asList(
                ColdFusion.class,
                ColdFusionSensor.class,
                ColdFusionSonarRulesDefinition.class,
                ColdFusionSonarWayProfileImporter.class,
                ColdFusionProfileExporter.class,
                ColdFusionCommonRulesEngine.class,
                ColdFusionCommonRulesDecorator.class
        );
    }

}
