package org.verapdf.tools.policy.generator;

public class PolicyHelper {
    public static final String START = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<sch:schema xmlns:sch=\"http://purl.oclc.org/dsdl/schematron\"\n" +
            "            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "            xsi:schemaLocation=\"http://purl.oclc.org/dsdl/schematron \">\n" +
            "\n" +
            "ISSUE_NUMBER_PART" +
            "    <!-- File: {fileNameToBeReplaced} -->\n" +
            "\n";
    
    public static final String ISSUE_NUMBER_PART = "    <!-- Issue# ISSUE_NUM -->\n" +
                    "    <!-- https://github.com/veraPDF/veraPDF-library/issues/ISSUE_NUM -->\n";
    public static final String PASS = START + 
            "    <sch:pattern name = \"Checking the validationReport: document is compliant\">\n" +
            "        <sch:rule context=\"/report/jobs/job/validationReport\">\n" +
            "            <sch:assert test=\"(@isCompliant = 'true')\">Failed check, Expected: isCompliant=true</sch:assert>\n" +
            "        </sch:rule>\n" +
            "    </sch:pattern>\n";
    public static final String FAIL = START +
            "    <sch:pattern name = \"Checking the validationReport: document is not compliant\">\n" +
            "        <sch:rule context=\"/report/jobs/job/validationReport\">\n" +
            "            <sch:assert test=\"(@isCompliant = 'false')\">Failed check, Expected: isCompliant=false</sch:assert>\n" +
            "        </sch:rule>\n" +
            "    </sch:pattern>\n" +
            "\n" +
            "    <sch:pattern name = \"Checking the validationReport: rules\">\n" +
            "        <sch:rule context=\"/report/jobs/job/validationReport/details\">\n" +
            "            <sch:assert test=\"(@failedRules = '{failedRulesToBeReplaced}')\">Failed check, Expected: {failedRulesToBeReplaced}</sch:assert>\t\n" +
            "        </sch:rule>\n" +
            "\n" +
            "        <sch:rule context=\"/report/jobs/job/validationReport/details/rule\">\n" +
            "            <sch:assert test=\"";
    public static final String RULE = "(@clause = '{ruleToBeReplaced}' and @testNumber = '{testNumToBeReplaced}' and @failedChecks = '{failedChecksCountToBeReplaced}')";
    public static final String OR = " or \n            ";
    public static final String RULE_MESSAGE = "{ruleToBeReplaced}-{testNumToBeReplaced}, {failedChecksCountToBeReplaced} check";
    public static final String RULE_END = "\">Failed rules, Expected: \n" +
            "            {messageToBeReplaced}</sch:assert>\n" +
            "        </sch:rule>\n" +
            "\n" +
            "    </sch:pattern>\n";
    public static final String LOGS_REPORT = "\n" +
            "    <sch:pattern name = \"Checking the logs\">\n" +
            "        <sch:rule context=\"/report/jobs/job\">\n" +
            "            <sch:assert test=\"count(logs) = 1\">Failed check, Expected: contains logs</sch:assert>\n" +
            "        </sch:rule>\n" +
            "\n" +
            "        <sch:rule context=\"/report/jobs/job/logs\">\n" +
            "            <sch:assert test=\"@logsCount = '{logsCountToBeReplaced}'\">Failed check, Expected: {logsCountToBeReplaced}</sch:assert>\t\n" +
            "        </sch:rule>\n";
    public static final String NO_LOGS = "\n    <sch:pattern name = \"Checking for the absence of logs\">\n" +
            "        <sch:rule context=\"/report/jobs/job\">\n" +
            "            <sch:assert test=\"not(logs)\">Failed check, Expected: no logs</sch:assert>\n" +
            "        </sch:rule>\n";
    public static final String LOGS = "\n" +
            "        <sch:rule context=\"/report/jobs/job/logs/logMessage\">\n" +
            "            <sch:assert test='";
    public static final String LOG = "(contains(., \"{logToBeReplaced}\") and @occurrences = \"{occurrencesToBeReplaced}\" and @level = \"{levelToBeReplaced}\")";
    public static final String LOG_MESSAGE = "'{levelToBeReplaced}: {logToBeReplaced}' with {occurrencesToBeReplaced} occurrences";

    static final String LOGS_END = "'>Invalid logs, Expected: \n" +
            "            {messageToBeReplaced}</sch:assert>\n" +
            "        </sch:rule>\n";
    static final String LOGS_REPORT_END = "    </sch:pattern>\n";
    public static final String EXC = START +
            "    <sch:pattern name = \"Checking the taskException\">\n" +
            "        <sch:rule context=\"/report/jobs/job/taskException\">\n" +
            "            <sch:assert test='contains(exceptionMessage, \"{exceptionMessageToBeReplaced}\")'>\n" +
            "                Failed check, Expected Error: {exceptionToBeReplaced}\n" +
            "            </sch:assert>\n" +
            "        </sch:rule>\n" +
            "    </sch:pattern>\n" +
            "\n" +
            "    <sch:pattern name = \"Checking the batchSummary\">\n" +
            "        <sch:rule context=\"/report/batchSummary\">\n" +
            "            <sch:assert test=\"(@totalJobs = '{totalJobsToBeReplaced}' and @failedToParse = '{failedToParseToBeReplaced}' " +
            "and @encrypted = '{encryptedToBeReplaced}' and @outOfMemory = '{outOfMemoryToBeReplaced}' and @veraExceptions = '{veraExceptionsToBeReplaced}')\">\n" +
            "                Failed check, Expected: totalJobs = '{totalJobsToBeReplaced}' failedToParse = '{failedToParseToBeReplaced}' " +
            "encrypted = '{encryptedToBeReplaced}' outOfMemory = '{outOfMemoryToBeReplaced}' veraExceptions = '{veraExceptionsToBeReplaced}'\n" +
            "            </sch:assert>\n" +
            "        </sch:rule>\n" +
            "    </sch:pattern>\n";
    ;
    public static final String END = "\n" +
            "</sch:schema>\n";
}
