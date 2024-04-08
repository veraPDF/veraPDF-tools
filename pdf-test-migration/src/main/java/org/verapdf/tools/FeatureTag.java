package org.verapdf.tools;

import java.util.ArrayList;
import java.util.List;

public enum FeatureTag {
    PRESET(0), PRE_PROCESS(1), PROCESS(2), POST_PROCESS(3);

    private final Integer value;

    private FeatureTag(Integer id) {
        this.value = 1 << id;
    }

    public Boolean checkTag(Integer tags) {
        return (this.value & tags) != 0;
    }

    public static Integer getInteger(FeatureTag... tags) {
        Integer result = 0;

        for (FeatureTag tag : tags) {
            if (!tag.checkTag(result)) {
                result += tag.value;
            }
        }

        return result;
    }

    public static List<FeatureTag> fromInteger(Integer value) {
        List<FeatureTag> tags = new ArrayList<FeatureTag>();

        for (FeatureTag tag : FeatureTag.values()) {
            if (tag.checkTag(value)) {
                tags.add(tag);
            }
        }

        return tags;
    }
}
