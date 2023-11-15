package org.verapdf.tools.policy.generator;

import org.apache.commons.cli.*;
import org.verapdf.core.VeraPDFException;
import org.verapdf.metadata.fixer.FixerFactory;
import org.verapdf.metadata.fixer.MetadataFixerConfig;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;
import org.verapdf.pdfa.validation.validators.BaseValidator;
import org.verapdf.pdfa.validation.validators.ValidatorFactory;
import org.verapdf.processor.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

public class PolicyGenerator {
    private static final String HELP = "[options] <FILE>\n Options:";
    private static final Logger logger = Logger.getLogger(PolicyGenerator.class.getCanonicalName());
    private Document document;
    private StringBuilder content;
    private String fileName;
    private String shortFilePath;
    private InputStream report;
    private ValidationProfile customProfile;
    private boolean isLogsEnabled = true;

    public PolicyGenerator() throws IOException {
        VeraGreenfieldFoundryProvider.initialise();
    }

    public static void main(String[] args) {
        Options options = defineOptions();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine commandLine;
        try {
            commandLine = (new DefaultParser()).parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp(HELP, options);
            return;
        }

        try {
            PolicyGenerator generator = new PolicyGenerator();
            if (commandLine.getArgs().length < 1) {
                formatter.printHelp(HELP, options);
                return;
            }
            if (commandLine.hasOption("n")) {
                generator.isLogsEnabled = false;
            }
            generator.fileName = String.join(" ", commandLine.getArgs());
            if (commandLine.hasOption("v")) {
                generator.validate(commandLine.getOptionValue("v"), commandLine.getOptionValue("profile"));
            } else {
                if (commandLine.hasOption("p")) {
                    String profilePath = commandLine.getOptionValue("profile");
                    if (profilePath != null) {
                        try (InputStream is = new FileInputStream(Paths.get(profilePath).toFile())) {
                            generator.customProfile = Profiles.profileFromXml(is);
                        } catch (JAXBException | FileNotFoundException e) {
                            generator.customProfile = null;
                            logger.log(Level.WARNING, "Error while getting profile from xml file. The profile will be selected automatically");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                generator.validate();
            }
            generator.generate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Options defineOptions() {
        Options options = new Options();
        Option isLogsChecked = new Option("n", "nologs", false, "Disables logs check");
        isLogsChecked.setRequired(false);
        options.addOption(isLogsChecked);
        Option profile = new Option("p", "profile", true, "Specifies path to custom profile");
        profile.setRequired(false);
        options.addOption(profile);
        Option verapdfPath = new Option("v", "verapdf_path", true, "path to verapdf");
        verapdfPath.setRequired(false);
        options.addOption(verapdfPath);
        return options;
    }

    private void validate(String verapdfPath, String profilePath) throws IOException {
        List<String> command = new LinkedList<>();
        List<String> veraPDFParameters = new LinkedList<>();
        if (isLogsEnabled) {
            veraPDFParameters.add("--addlogs");
        }
        if (profilePath != null) {
            veraPDFParameters.add("--profile");
            veraPDFParameters.add(profilePath);
        }

        File tempMrrFile = File.createTempFile("veraPDF", ".mrr");
        tempMrrFile.deleteOnExit();
        veraPDFParameters.add("1>" + tempMrrFile.getAbsolutePath());
        command.add(verapdfPath);
        command.addAll(veraPDFParameters);
        command.add(fileName);

        command = command.stream().map(parameter -> {
            if (parameter.isEmpty()) {
                return "\"\"";
            }
            return parameter;
        }).collect(toList());

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(command);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = pb.start();
            process.waitFor();
            report = new FileInputStream(tempMrrFile);
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    private void validate() throws IOException {
        MetadataFixerConfig fixConf = FixerFactory.configFromValues("test");

        ProcessorConfig processorConfig = this.customProfile == null
                ? ProcessorFactory.fromValues(
                ValidatorFactory.createConfig(PDFAFlavour.NO_FLAVOUR, PDFAFlavour.PDFA_1_B, true,
                        0, false, isLogsEnabled, Level.WARNING, BaseValidator.DEFAULT_MAX_NUMBER_OF_DISPLAYED_FAILED_CHECKS, false, "", false, false),
                null, null, fixConf, EnumSet.of(TaskType.VALIDATE), (String) null)
                : ProcessorFactory.fromValues(ValidatorFactory.createConfig(PDFAFlavour.NO_FLAVOUR,
                PDFAFlavour.NO_FLAVOUR, true, 0, false, isLogsEnabled, Level.WARNING,
                BaseValidator.DEFAULT_MAX_NUMBER_OF_DISPLAYED_FAILED_CHECKS, false, "", false, false),
                null, null, fixConf, EnumSet.of(TaskType.VALIDATE), this.customProfile, null);

        BatchProcessor processor = ProcessorFactory.fileBatchProcessor(processorConfig);

        File tempMrrFile = File.createTempFile("veraPDF", ".mrr");
        tempMrrFile.deleteOnExit();

        List<File> files = new ArrayList<>();
        files.add(Paths.get(fileName).toFile());
        try (OutputStream reportStream = new FileOutputStream(tempMrrFile)) {
            processor.process(files, ProcessorFactory.getHandler(FormatOption.MRR, false, reportStream, false));
            report = new FileInputStream(tempMrrFile);
        } catch (IOException | VeraPDFException e) {
            e.printStackTrace();
        }
    }

    public void generate() {
        try {
            document = (DocumentBuilderFactory.newInstance().newDocumentBuilder()).parse(report);

            String[] name = fileName.split("/");
            shortFilePath = name[name.length - 1];
            Path policy = Paths.get(fileName.replace(".pdf", ".sch"));
            NodeList nodeList = document.getElementsByTagName("validationReport");

            if (nodeList.getLength() == 0) {
                generateExceptionPolicy();
            } else {
                String isCompliant = nodeList.item(0).getAttributes().getNamedItem("isCompliant").getNodeValue();
                if ("true".equals(isCompliant)) {
                    generatePassPolicy();
                } else {
                    generateFailPolicy();
                }
            }

            if (isLogsEnabled) {
                appendLogs();
            }
            content.append(PolicyHelper.END);

            Files.write(policy, content.toString().getBytes());
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    private void generateFailPolicy() {
        NodeList nodeList = document.getElementsByTagName("details");
        String failedRulesToBeReplaced = nodeList.item(0).getAttributes().getNamedItem("failedRules").getNodeValue();

        content = new StringBuilder(PolicyHelper.FAIL
                .replace("{fileNameToBeReplaced}", shortFilePath)
                .replace("ISSUE_NUM", shortFilePath.split("_")[0])
                .replace("{failedRulesToBeReplaced}", failedRulesToBeReplaced));

        nodeList = document.getElementsByTagName("rule");
        int size = nodeList.getLength();
        StringBuilder messageToBeReplaced = new StringBuilder();
        SortedSet<RuleInfo> ruleInfoSet = new TreeSet<>();
        for (int i = 0; i < size; ++i) {
            NamedNodeMap node = nodeList.item(i).getAttributes();
            ruleInfoSet.add(new RuleInfo(node.getNamedItem("clause").getNodeValue(),
                    Integer.parseInt(node.getNamedItem("testNumber").getNodeValue()),
                    Integer.parseInt(node.getNamedItem("failedChecks").getNodeValue())));
        }
        Iterator<RuleInfo> iterator = ruleInfoSet.iterator();
        while (iterator.hasNext()) {
            RuleInfo ruleInfo = iterator.next();
            content.append(PolicyHelper.RULE
                    .replace("{ruleToBeReplaced}", ruleInfo.getRuleId().getClause())
                    .replace("{testNumToBeReplaced}", String.valueOf(ruleInfo.getRuleId().getTestNumber()))
                    .replace("{failedChecksCountToBeReplaced}", String.valueOf(ruleInfo.getFailedChecks())));

            messageToBeReplaced.append(PolicyHelper.RULE_MESSAGE
                    .replace("{ruleToBeReplaced}",  ruleInfo.getRuleId().getClause())
                    .replace("{testNumToBeReplaced}", String.valueOf(ruleInfo.getRuleId().getTestNumber()))
                    .replace("{failedChecksCountToBeReplaced}", String.valueOf(ruleInfo.getFailedChecks())));
            if (ruleInfo.getFailedChecks() > 1) {
                messageToBeReplaced.append("s");
            }
            if (iterator.hasNext()) {
                content.append(PolicyHelper.OR);
                messageToBeReplaced.append(",").append(PolicyHelper.OR);
            }

        }
        content.append(PolicyHelper.RULE_END
                .replace("{messageToBeReplaced}", messageToBeReplaced));

        System.out.println("Policy was created. PDF file is not compliant with Validation Profile requirements");
    }

    private void generatePassPolicy() {
        content = new StringBuilder(PolicyHelper.PASS
                .replace("{fileNameToBeReplaced}", shortFilePath)
                .replace("ISSUE_NUM", shortFilePath.split("_")[0]));

        System.out.println("Policy was created. PDF file is compliant with Validation Profile requirements");
    }

    private void generateExceptionPolicy() {
        NodeList nodeList = document.getElementsByTagName("exceptionMessage");
        String exceptionToBeReplaced = nodeList.item(0).getFirstChild().getNodeValue()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        String exceptionMessageToBeReplaced = exceptionToBeReplaced.replace("'", "&apos;");

        NamedNodeMap node = document.getElementsByTagName("batchSummary").item(0).getAttributes();
        String totalJobsToBeReplaced = node.getNamedItem("totalJobs").getNodeValue();
        String failedToParseToBeReplaced = node.getNamedItem("failedToParse").getNodeValue();
        String encryptedToBeReplaced = node.getNamedItem("encrypted").getNodeValue();
        String outOfMemoryToBeReplaced = node.getNamedItem("outOfMemory").getNodeValue();
        String veraExceptionsToBeReplaced = node.getNamedItem("veraExceptions").getNodeValue();
        content = new StringBuilder(PolicyHelper.EXC
                .replace("{fileNameToBeReplaced}", shortFilePath)
                .replace("ISSUE_NUM", shortFilePath.split("_")[0])
                .replace("{exceptionMessageToBeReplaced}", exceptionMessageToBeReplaced)
                .replace("{exceptionToBeReplaced}", exceptionToBeReplaced)
                .replace("{totalJobsToBeReplaced}", totalJobsToBeReplaced)
                .replace("{failedToParseToBeReplaced}", failedToParseToBeReplaced)
                .replace("{encryptedToBeReplaced}", encryptedToBeReplaced)
                .replace("{outOfMemoryToBeReplaced}", outOfMemoryToBeReplaced)
                .replace("{veraExceptionsToBeReplaced}", veraExceptionsToBeReplaced)
        );

        System.out.println("Policy was created. Could not complete validation due to an error");
    }

    private void appendLogs() {
        NodeList nodeList = document.getElementsByTagName("logs");
        if (nodeList.getLength() == 0) {
            content.append(PolicyHelper.NO_LOGS);
        } else {
            String logsCountToBeReplaced = nodeList.item(0).getAttributes().getNamedItem("logsCount").getNodeValue();
            content.append(PolicyHelper.LOGS_REPORT
                    .replace("{logsCountToBeReplaced}", logsCountToBeReplaced));

            nodeList = document.getElementsByTagName("logMessage");
            int size = nodeList.getLength();
            if (size > 0) {
                content.append(PolicyHelper.LOGS);
                StringBuilder messageToBeReplaced = new StringBuilder();
                SortedSet<LogInfo> logInfoSet = new TreeSet<>();
                for (int i = 0; i < size; ++i) {
                    Node node = nodeList.item(i);
                    logInfoSet.add(new LogInfo(Level.parse(node.getAttributes().getNamedItem("level").getNodeValue()),
                            node.getFirstChild().getNodeValue()
                                    .replace("&", "&amp;")
                                    .replace("<", "&lt;")
                                    .replace(">", "&gt;"),
                            Integer.parseInt(node.getAttributes().getNamedItem("occurrences").getNodeValue())));
                }
                Iterator<LogInfo> iterator = logInfoSet.iterator();
                while (iterator.hasNext()) {
                    LogInfo logInfo = iterator.next();
                    content.append(PolicyHelper.LOG
                            .replace("{logToBeReplaced}", logInfo.getMessage().replace("'", "&apos;")
                                    .replace(shortFilePath, ".pdf"))
                            .replace("{occurrencesToBeReplaced}", String.valueOf(logInfo.getOccurrences()))
                            .replace("{levelToBeReplaced}", logInfo.getLevel().getName()));

                    messageToBeReplaced.append(PolicyHelper.LOG_MESSAGE
                            .replace("{logToBeReplaced}", logInfo.getMessage())
                            .replace("{occurrencesToBeReplaced}", String.valueOf(logInfo.getOccurrences()))
                            .replace("{levelToBeReplaced}", logInfo.getLevel().getName()));

                    if (iterator.hasNext()) {
                        content.append(PolicyHelper.OR);
                        messageToBeReplaced.append(",").append(PolicyHelper.OR);
                    }
                }
                content.append(PolicyHelper.LOGS_END
                        .replace("{messageToBeReplaced}", messageToBeReplaced));
            }
        }
        content.append(PolicyHelper.LOGS_REPORT_END);
    }
}
