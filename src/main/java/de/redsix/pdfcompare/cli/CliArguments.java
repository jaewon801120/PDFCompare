package de.redsix.pdfcompare.cli;

import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.CompareResultImpl;
import de.redsix.pdfcompare.CompareResultImpl_SHLife;
import de.redsix.pdfcompare.PdfComparator;
import de.redsix.pdfcompare.env.ConfigFileEnvironment;
import de.redsix.pdfcompare.env.SimpleEnvironment;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class CliArguments {

    private static final Logger LOG = LoggerFactory.getLogger(CliArguments.class);

    private static final int EXPECTED_FILENAME_INDEX = 0;
    private static final int ACTUAL_FILENAME_INDEX = 1;

    private static final int EQUAL_DOCUMENTS_RESULT_VALUE = 0;
    private static final int UNEQUAL_DOCUMENTS_RESULT_VALUE = 1;
    private static final int ERROR_RESULT_VALUE = 2;

    private static final String OUTPUT_OPTION = "o";
    private static final String EXCLUSION_OPTION = "x";
    private static final String HELP_OPTION = "h";
    private static final String EXPECTED_PASSWORD_OPTION = "exppwd";
    private static final String ACTUAL_PASSWORD_OPTION = "actpwd";

    private final Options options;
    private CommandLine commandLine;

    public CliArguments(String[] args) {
        options = new Options();
        options.addOption(Option.builder(HELP_OPTION)
                .argName("help")
                .desc("Displays this text and exit")
                .hasArg(false)
                .longOpt("help")
                .numberOfArgs(0)
                .required(false)
                .build());
        options.addOption(Option.builder(OUTPUT_OPTION)
                .argName("output")
                .desc("Provide an optional output file for the result")
                .hasArg(true)
                .longOpt("output")
                .numberOfArgs(1)
                .required(false)
                .type(String.class)
                .valueSeparator('=')
                .build());
        options.addOption(Option.builder(EXCLUSION_OPTION)
                .argName("exclusions")
                .desc("Provide an optional file with exclusions")
                .hasArg(true)
                .longOpt("exclusions")
                .numberOfArgs(1)
                .required(false)
                .type(String.class)
                .valueSeparator('=')
                .build());
        options.addOption(Option.builder(EXPECTED_PASSWORD_OPTION)
                .argName("expected-password")
                .desc("Provide a password for the expected file")
                .hasArg(true)
                .longOpt("expected-password")
                .numberOfArgs(1)
                .required(false)
                .type(String.class)
                .valueSeparator('=')
                .build());
        options.addOption(Option.builder(ACTUAL_PASSWORD_OPTION)
                .argName("actual-password")
                .desc("Provide a password for the actual file")
                .hasArg(true)
                .longOpt("actual-password")
                .numberOfArgs(1)
                .required(false)
                .type(String.class)
                .valueSeparator('=')
                .build());
        process(args);
    }

    public boolean hasFileArguments() {
        return commandLine.getArgList().size() == 2 && getExpectedFile().isPresent() && getActualFile().isPresent();
    }

    private boolean isHelp() {
        return commandLine.hasOption(HELP_OPTION);
    }

    public Optional<String> getExpectedFile() {
        return getRemainingArgument(EXPECTED_FILENAME_INDEX);
    }

    public Optional<String> getActualFile() {
        return getRemainingArgument(ACTUAL_FILENAME_INDEX);
    }

    public  Optional<String> getExclusionsFile() {
        return Optional.ofNullable(commandLine.getOptionValue(EXCLUSION_OPTION));
    }

    public Optional<String> getOutputFile() {
        return Optional.ofNullable(commandLine.getOptionValue(OUTPUT_OPTION));
    }

    public Optional<String> getExpectedPassword() {
        return Optional.ofNullable(commandLine.getOptionValue(EXPECTED_PASSWORD_OPTION));
    }

    public Optional<String> getActualPassword() {
        return Optional.ofNullable(commandLine.getOptionValue(ACTUAL_PASSWORD_OPTION));
    }

    /*package*/ int printHelp() {
        new HelpFormatter().printHelp("java -jar pdfcompare-x.x.x-full.jar [EXPECTED] [ACTUAL]\n" +
                "\n" +
                "Return codes:\n" +
                "   0 for documents are equal\n" +
                "   1 for documents differ\n" +
                "   2 for errors occured\n\n", options);
        return 0;
    }

    public int execute() {
        if (isHelp()) {
            return printHelp();
        }
        if (hasFileArguments()) {
            return doCompare_SHLife();
        }
        System.out.println("No files or too many files where passed as arguments\n");
        printHelp();
        return ERROR_RESULT_VALUE;
    }

    private int doCompare_SHLife() {
        try {
            PdfComparator<CompareResultImpl> pdfComparator = new PdfComparator<>(getExpectedFile().get(), getActualFile().get(), new CompareResultImpl_SHLife());

        	File fConfig = new File("D:\\Download\\jar\\PDF\\reference.conf");
        	if (!fConfig.isFile()) {
        		SimpleEnvironment simpleEnv = new SimpleEnvironment();
        		simpleEnv.setNrOfImagesToCache(30);
        		simpleEnv.setMaxImageSize(100000);
        		simpleEnv.setMergeCacheSize(100);
        		simpleEnv.setSwapCacheSize(100);
        		simpleEnv.setDocumentCacheSize(200);
        		simpleEnv.setParallelProcessing(false);
        		simpleEnv.setOverallTimeout(15);
        		simpleEnv.setAllowedDiffInPercent(0);
        		simpleEnv.setDPI(100);
        		simpleEnv.setAddEqualPagesToResult(false);
        		simpleEnv.setFailOnMissingIgnoreFile(false);
        		simpleEnv.setAntiAliasing(false);
        		
        		pdfComparator.withEnvironment(simpleEnv);
        	}
        	else {
        		ConfigFileEnvironment configEnv = new ConfigFileEnvironment(fConfig);
            	pdfComparator.withEnvironment(configEnv);
        	}
        	
            //System.setProperty("config.file", "D:\\repository\\PDFCompare\\src\\main\\resources\\reference.conf");
            getExclusionsFile().ifPresent(pdfComparator::withIgnore);
            getExpectedPassword().ifPresent(pdfComparator::withExpectedPassword);
            getActualPassword().ifPresent(pdfComparator::withActualPassword);
            System.out.println("compare");
            CompareResult compareResult = pdfComparator.compare();
            System.out.println("write");
            getOutputFile().ifPresent(compareResult::writeTo);
            System.out.println(String.valueOf(compareResult.isEqual()));
            return (compareResult.isEqual()) ? EQUAL_DOCUMENTS_RESULT_VALUE : UNEQUAL_DOCUMENTS_RESULT_VALUE;
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
            return ERROR_RESULT_VALUE;
        }
    }

    private int doCompare() {
        try {
            PdfComparator<CompareResultImpl> pdfComparator = new PdfComparator<>(getExpectedFile().get(), getActualFile().get());
            getExclusionsFile().ifPresent(pdfComparator::withIgnore);
            getExpectedPassword().ifPresent(pdfComparator::withExpectedPassword);
            getActualPassword().ifPresent(pdfComparator::withActualPassword);
            CompareResult compareResult = pdfComparator.compare();
            getOutputFile().ifPresent(compareResult::writeTo);
            return (compareResult.isEqual()) ? EQUAL_DOCUMENTS_RESULT_VALUE : UNEQUAL_DOCUMENTS_RESULT_VALUE;
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
            return ERROR_RESULT_VALUE;
        }
    }

    private void process(String[] args) {
        try {
            commandLine = new DefaultParser().parse(options, args);
        } catch (ParseException exception) {
            throw new CliArgumentsParseException(exception);
        }
    }

    private Optional<String> getRemainingArgument(int index) {
        if (commandLine.getArgList().isEmpty() || commandLine.getArgList().size() <= index) {
            return Optional.empty();
        }
        return Optional.of(commandLine.getArgList().get(index));
    }
}
