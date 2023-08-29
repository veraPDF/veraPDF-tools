package org.verapdf.tools.policy.generator;

import java.util.logging.Level;

public class LogInfo implements Comparable<LogInfo> {
    private final Level level;

    private final String message;
    private final Integer occurrences;

    public LogInfo(Level level, String message, Integer occurrences) {
        this.level = level;
        this.message = message;
        this.occurrences = occurrences;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public Integer getOccurrences() {
        return occurrences;
    }

    @Override
    public int compareTo(LogInfo logInfo) {
        int res = Integer.compare(logInfo.level.intValue(), this.level.intValue());
        if (res != 0) {
            return res;
        }
        return this.message.compareTo(logInfo.getMessage());
    }
 }
