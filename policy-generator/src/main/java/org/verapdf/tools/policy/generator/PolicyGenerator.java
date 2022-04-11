package org.verapdf.tools.policy.generator;

import org.verapdf.core.VeraPDFException;
import org.verapdf.metadata.fixer.FixerFactory;
import org.verapdf.metadata.fixer.MetadataFixerConfig;
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PolicyGenerator {
    private static final Logger logger = Logger.getLogger(PolicyGenerator.class.getCanonicalName());
    private Document document;
    private StringBuilder content;
    private String fileName;
    private String shortFilePath;
    private InputStream report;
    private ValidationProfile customProfile;
    private boolean isLogsEnabled;

    public PolicyGenerator() throws IOException {
        VeraGreenfieldFoundryProvider.initialise();
    }

    public static void main(String[] args) {
        try {
            (new PolicyGenerator()).run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() throws IOException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Enter pdf file and profile (optional, space separated) paths (or \"exit\"): ");
            String paths = scanner.nextLine();
            if ("exit".equals(paths)) {
                break;
            }
            customProfile = null;
            String profilePath;
            if (paths.contains(".xml")) {
                fileName = paths.split("\\.pdf ")[0] + ".pdf";
                profilePath = paths.split("\\.pdf ")[1];
            } else {
                fileName = paths;
                profilePath = null;
            }
            if (profilePath != null) {
                try (InputStream is = new FileInputStream(Paths.get(profilePath).toFile())) {
                    customProfile = Profiles.profileFromXml(is);
                } catch (JAXBException | FileNotFoundException e) {
                    customProfile = null;
                    logger.log(Level.WARNING, "Error while getting profile from xml file. The profile will be selected automatically");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Is logs enabled? true / false ");
            isLogsEnabled = scanner.nextLine().contains("true");
            validate();
            generate();
        }
    }

    private void validate() throws IOException {
        MetadataFixerConfig fixConf = FixerFactory.configFromValues("test", true);

        ProcessorConfig processorConfig = this.customProfile == null
                ? ProcessorFactory.fromValues(
                ValidatorFactory.createConfig(PDFAFlavour.NO_FLAVOUR, PDFAFlavour.PDFA_1_B, true, 0, false, isLogsEnabled, Level.WARNING, BaseValidator.DEFAULT_MAX_NUMBER_OF_DISPLAYED_FAILED_CHECKS),
                null, null, fixConf, EnumSet.of(TaskType.VALIDATE), (String) null)
                : ProcessorFactory.fromValues(
                ValidatorFactory.createConfig(PDFAFlavour.NO_FLAVOUR, PDFAFlavour.NO_FLAVOUR, true, 0, false, isLogsEnabled, Level.WARNING, BaseValidator.DEFAULT_MAX_NUMBER_OF_DISPLAYED_FAILED_CHECKS),
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

            if (isLogsEnabled && document.getElementsByTagName("logs").getLength() != 0) {
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
        for (int i = 0; i < size; ++i) {
            NamedNodeMap node = nodeList.item(i).getAttributes();
            String ruleToBeReplaced = node.getNamedItem("clause").getNodeValue();
            String testNumToBeReplaced = node.getNamedItem("testNumber").getNodeValue();
            String failedChecksCountToBeReplaced = node.getNamedItem("failedChecks").getNodeValue();

            content.append(PolicyHelper.RULE
                    .replace("{ruleToBeReplaced}", ruleToBeReplaced)
                    .replace("{testNumToBeReplaced}", testNumToBeReplaced)
                    .replace("{failedChecksCountToBeReplaced}", failedChecksCountToBeReplaced));

            messageToBeReplaced.append(PolicyHelper.RULE_MESSAGE
                    .replace("{ruleToBeReplaced}", ruleToBeReplaced)
                    .replace("{testNumToBeReplaced}", testNumToBeReplaced)
                    .replace("{failedChecksCountToBeReplaced}", failedChecksCountToBeReplaced));
            if (Integer.parseInt(failedChecksCountToBeReplaced) > 1) {
                messageToBeReplaced.append("s");
            }
            if (i != size - 1) {
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
        String logsCountToBeReplaced = nodeList.item(0).getAttributes().getNamedItem("logsCount").getNodeValue();
        content.append(PolicyHelper.LOGS_REPORT
                .replace("{logsCountToBeReplaced}", logsCountToBeReplaced));

        nodeList = document.getElementsByTagName("logMessage");
        int size = nodeList.getLength();
        if (size > 0) {
            content.append(PolicyHelper.LOGS);
            StringBuilder messageToBeReplaced = new StringBuilder();
            for (int i = 0; i < size; ++i) {
                Node node = nodeList.item(i);
                String logToBeReplaced = node.getFirstChild().getNodeValue()
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;");
                String occurrencesToBeReplaced = node.getAttributes().getNamedItem("occurrences").getNodeValue();
                String levelToBeReplaced = node.getAttributes().getNamedItem("level").getNodeValue();
                content.append(PolicyHelper.LOG
                        .replace("{logToBeReplaced}", logToBeReplaced.replace("'", "&apos;"))
                        .replace("{occurrencesToBeReplaced}", occurrencesToBeReplaced)
                        .replace("{levelToBeReplaced}", levelToBeReplaced));

                messageToBeReplaced.append(PolicyHelper.LOG_MESSAGE
                        .replace("{logToBeReplaced}", logToBeReplaced)
                        .replace("{occurrencesToBeReplaced}", occurrencesToBeReplaced)
                        .replace("{levelToBeReplaced}", levelToBeReplaced));

                if (i != size - 1) {
                    content.append(PolicyHelper.OR);
                    messageToBeReplaced.append(",").append(PolicyHelper.OR);
                }
            }
            content.append(PolicyHelper.LOGS_END
                    .replace("{messageToBeReplaced}", messageToBeReplaced));
        }
        content.append(PolicyHelper.LOGS_REPORT_END);
    }
}
