package org.verapdf.tools;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.verapdf.pdfa.flavours.PDFAFlavour;

public class TestMigration {
    private static final String HELP = "[options] <INPUT_FILE> <OUTPUT_FILE> OR [options] <INPUT_FOLDER> <INPUT_VERSION> <OUTPUT_FOLDER> <OUTPUT_VERSION> OR [options] -csv <CSV_FILE>\n Options:\n";
    private static final Integer FORMATTER_WIDTH = 140;
    private static final Logger logger = Logger.getLogger(TestMigration.class.getCanonicalName());

    public static void main(String[] args) {
        try {
            TestMigration testMigration = new TestMigration();

            testMigration.run(args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected PDFAFlavour targetFlavour = PDFAFlavour.NO_FLAVOUR;
    protected String inputParentFolderPath = "";
    protected String targetParentFolderPath = "";

    private CommandLine commandLine;
    private HelpFormatter formatter = new HelpFormatter();
    private Features features;
    private List<Task> tasks = new ArrayList<Task>();
    private Boolean isFromCSV = false;

    public TestMigration() {

    }

    public Features getFeatures() {
        return features;
    }

    public Path getTempDir() {
        return Paths.get(System.getProperty("user.dir") + "\\fixed_files");
    }

    private void clearTemp() {
        File folder = getTempDir().toFile();

        if (folder == null || !folder.exists()) {
            return;
        }

        for (File file : folder.listFiles()) {
            file.delete();
        }

        folder.delete();
    }

    protected void setCSVFile(String filePath) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new FileInputStream(new File(filePath)));
            Integer pos = 0;

            while (scanner.hasNext()) {
                String[] args = scanner.nextLine().split(";");

                if (args.length < 2) {
                    throw new ParseException("line:" + pos + " - have less then 2 arguments");
                }

                addSingleTask(args);
            }

            isFromCSV = true;
        } catch (Exception ex) {
            logger.warning("Error during parsing csv file: " + ex.getMessage() + "");
            ex.printStackTrace();
            System.exit(1);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private void run(String inputArgs[]) {
        Options options = OptionFeature.generateOptions();
        commandLine = null;

        formatter.setWidth(FORMATTER_WIDTH);

        try {
            commandLine = (new DefaultParser()).parse(options, inputArgs);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp(HELP, options);

            return;
        }

        if (!isInputValid()) {
            formatter.printHelp(HELP, options);

            return;
        }

        clearTemp();

        features = new Features(commandLine);
        features.checkPresets(this);
        features.runPreFeatures(this);

        if (!isFromCSV) {
            addSingleTask(commandLine.getArgs());
        }

        for (Task task : tasks) {
            logger.info("task - " + task.inputVersion + ":");

            if (!task.isSingleFile) {
                proccessTask(task);

                continue;
            }

            proccessSingleFileTask(task);
        }

        clearTemp();
    }

    private void proccessSingleFileTask(Task task) {
        File inputFile = Paths.get(inputParentFolderPath + task.inputPath).toFile();
        File targetFile = Paths.get(targetParentFolderPath + task.targetPath).toFile();

        File parent = targetFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try {
            PDDocument document = PDDocument.load(inputFile);
            features.runFeatures(document);

            if (task.outlineHeader != null && task.outlineHeader.length() > 0) {
                OptionFeature.SET_OUTLINEHEADER.feature(document, task.outlineHeader);
            }

            if (targetFile.exists()) {
                targetFile.delete();
            }

            document.save(targetFile);

            features.runPostFeatures(this, targetFile.getAbsolutePath());
        } catch (Exception e) {
            logger.warning(">Error during processing: " + inputFile.getName() + ", test skiped.");
            e.printStackTrace();
        }
    }

    private void proccessTask(Task task) {
        File inputFolder = Paths.get(inputParentFolderPath + task.inputPath).toFile();
        File targetFolder = Paths.get(targetParentFolderPath + task.targetPath).toFile();

        if (!inputFolder.exists()) {
            logger.warning(">Input folder '" + inputFolder.getAbsolutePath() + "'' not found, tests skipped");
            return;
        }

        targetFolder.mkdirs();

        File[] inputFiles = inputFolder.listFiles((dir, name) -> {
            return (name.startsWith(task.inputVersion) && name.endsWith(".pdf"));
        });

        for (File inputFile : inputFiles) {
            String inputFileName = inputFile.getName();
            // file format '[version]-[fail/pass]-[testCase].pdf'
            String suffix = inputFileName.substring(task.inputVersion.length());

            String targetFileName = task.targetVersion + suffix;
            String targetFilePath = targetParentFolderPath + task.targetPath + '/' + targetFileName;

            try {
                PDDocument document = PDDocument.load(inputFile);
                features.runFeatures(document);

                if (task.outlineHeader != null && task.outlineHeader.length() > 0) {
                    OptionFeature.SET_OUTLINEHEADER.feature(document, task.outlineHeader);
                }

                File targetFile = new File(targetFilePath);

                if (targetFile.exists()) {
                    targetFile.delete();
                }

                document.save(targetFile);

                features.runPostFeatures(this, targetFilePath);
            } catch (Exception e) {
                logger.warning(">Error during processing: " + inputFileName + ", test skiped.");
                e.printStackTrace();

                continue;
            }

            File targetFile = new File(targetFilePath);

            if (!targetFile.exists()) {
                logger.warning(">Error during processing: " + inputFileName + ", test skiped.");
                continue;
            }

            logger.info(">Migrated (" + inputFileName + ") -> (" + targetFileName + ")");
        }
    }

    private void addSingleTask(String[] args) {
        if (args.length < 4) {
            addSingleFileTask(args);

            return;
        }

        String inputPath = args[0];
        String inputVersion = args[1];
        String targetPath = args[2];
        String targetVersion = args[3];
        String outlineHeader = "";

        if (inputVersion.equals("all")) {
            inputVersion = "";
        }

        if (targetVersion.equals("all")) {
            targetVersion = "";
        }

        if (args.length > 4) {
            outlineHeader = args[4];
        }

        tasks.add(new Task(inputPath, inputVersion, targetPath, targetVersion, outlineHeader, false));
    }

    private void addSingleFileTask(String[] args) {
        String inputPath = args[0];
        String targetPath = args[1];
        String outlineHeader = "";

        if (args.length > 2) {
            outlineHeader = args[2];
        }

        File file = new File(inputPath);

        if (!file.exists()) {
            logger.warning("Input file '" + inputPath + "'' not found");
            System.exit(1);

            return;
        }

        File targetFile = new File(targetPath);

        tasks.add(new Task(inputPath, file.getName(), targetPath, targetFile.getName(), outlineHeader, true));
    }

    private Boolean isInputValid() {
        for (OptionFeature optionFeature : OptionFeature.values()) {
            if (optionFeature.isRequired() && !commandLine.hasOption(optionFeature.getShortOption())) {
                return false;
            }
        }

        return (commandLine.getArgs().length == 2 || commandLine.getArgs().length == 4
                || commandLine.hasOption(OptionFeature.CSV_FILE.getShortOption()));
    }
}
