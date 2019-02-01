/*
Copyright 2016 StepStone GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.stepstone.sonar.plugin.coldfusion;

import com.google.common.base.Preconditions;
import com.stepstone.sonar.plugin.coldfusion.cflint.CFLintAnalyzer;
import com.stepstone.sonar.plugin.coldfusion.cflint.CFlintAnalysisResultImporter;
import com.stepstone.sonar.plugin.coldfusion.cflint.CFlintConfigExporter;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ColdFusionSensor implements Sensor {

    private final FileSystem fs;
    private final RulesProfile ruleProfile;
    private final Logger LOGGER = Loggers.get(ColdFusionSensor.class);

    public ColdFusionSensor(FileSystem fs, RulesProfile ruleProfile) {
        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(ruleProfile);

        this.fs = fs;
        this.ruleProfile = ruleProfile;
    }


    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.onlyOnLanguage(ColdFusionPlugin.LANGUAGE_KEY);
        descriptor.createIssuesForRuleRepository(ColdFusionPlugin.REPOSITORY_KEY);
    }

    @Override
    public void execute(SensorContext context) {
        try {
            analyze(context);
            importResults(context);
            measureProcessor(context);
        } catch (IOException | XMLStreamException e) {
            LOGGER.error("",e);
        }
    }

    private void analyze(SensorContext context) throws IOException, XMLStreamException {
        File configFile = generateCflintConfig();
        new CFLintAnalyzer(context).analyze(configFile);
        //when analysis is done we delete the created file
        deleteFile(configFile);
    }

    private File generateCflintConfig() throws IOException, XMLStreamException {
        final File configFile = new File(fs.workDir(), "cflint-config.xml");
        new CFlintConfigExporter(ruleProfile).save(configFile);
        return configFile;
    }

    private void deleteFile(File configFile) throws IOException {
        if(configFile!= null){
           Files.deleteIfExists(configFile.toPath());
        }
    }

    private void importResults(SensorContext sensorContext) throws IOException {
        try {
            new CFlintAnalysisResultImporter(fs, sensorContext).parse(new File(fs.workDir(), "cflint-result.xml"));
        } catch (XMLStreamException e) {
            LOGGER.error(",e");
        } finally {
            deleteFile(new File(fs.workDir(), "cflint-result.xml"));
        }
    }

    private void measureProcessor(SensorContext context) {
        LOGGER.info("Starting measure processor");

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Callable<Integer>> callableTasks = new ArrayList<>();

        for (InputFile inputFile : fs.inputFiles(fs.predicates().hasLanguage(ColdFusionPlugin.LANGUAGE_KEY))) {
            Callable<Integer> callableTask = () -> {
                try {
                    metricsLinesCounter(inputFile, context);
                    return 1;
                } catch (IOException e) {
                    return 0;
                }
            };
            callableTasks.add(callableTask);
        }

        try {
            executorService.invokeAll(callableTasks);
            executorService.shutdown();
            executorService.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOGGER.error("",e);
        }

        LOGGER.info("Measure processor done");
    }

    //Very basic and naive line of code counter for Coldfusion
    //Might count a line of code as comment
    private void metricsLinesCounter(InputFile inputFile, SensorContext context) throws IOException {
        String currentLine;
        int commentLines = 0;
        int blankLines = 0;
        int lines = 0;
        Metric metricLinesOfCode = CoreMetrics.NCLOC;
        Metric metricLines = CoreMetrics.LINES;
        Metric metricCommentLines = CoreMetrics.COMMENT_LINES;
        if(inputFile==null){
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputFile.inputStream()))) {
            if (inputFile.inputStream() != null) {
                while ((currentLine = reader.readLine()) != null) {
                    lines++;
                    if (currentLine.contains("<!--")) {
                        commentLines++;
                        if (currentLine.contains("-->")) {
                            continue;
                        }
                        commentLines++;
                        lines++;
                        while (!(reader.readLine()).contains("-->")) {
                            lines++;
                            commentLines++;
                        }
                    } else if (currentLine.trim().isEmpty()) {
                        blankLines++;
                    }
                }
            }
        }

        context.newMeasure().forMetric(metricCommentLines).on(inputFile).withValue(commentLines).save();
        context.newMeasure().forMetric(metricLinesOfCode).on(inputFile).withValue(lines-blankLines-commentLines).save();
        context.newMeasure().forMetric(metricLines).on(inputFile).withValue(lines).save();
    }

}

