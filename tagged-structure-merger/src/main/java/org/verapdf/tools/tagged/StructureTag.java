package org.verapdf.tools.tagged;

import org.verapdf.tools.TaggedPDFConstants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public enum StructureTag {
    
    STRUCTURE_TAG("structure", new String[]{}, null),
    TABLE_TAG("table", new String[]{TaggedPDFConstants.TABLE, TaggedPDFConstants.TD, TaggedPDFConstants.TH,
            TaggedPDFConstants.THEAD, TaggedPDFConstants.TBODY, TaggedPDFConstants.TFOOT}),
    LIST_TAG("list", new String[]{TaggedPDFConstants.L, TaggedPDFConstants.LI, TaggedPDFConstants.LBODY}),
    SPAN_TAG("span", new String[]{TaggedPDFConstants.SPAN}),
    HEADING_TAG("heading", new String[]{TaggedPDFConstants.H}, TaggedPDFConstants.HN_REGEXP),
    FIGURE_TAG("figure", new String[]{TaggedPDFConstants.FIGURE}),
    CAPTION_TAG("caption", new String[]{TaggedPDFConstants.CAPTION}),
    TOC_TAG("toc", new String[]{TaggedPDFConstants.TOC, TaggedPDFConstants.TOCI}),
    NOTE_TAG("note", new String[]{TaggedPDFConstants.NOTE, TaggedPDFConstants.FENOTE}),
    PARAGRAPH_TAG("paragraph", new String[]{TaggedPDFConstants.P});
    
    private final String tag;
    private final Set<String> structureTags;
    private final String regex;

    StructureTag(String tag, String[] structureTags, String regex) {
        this.tag = tag;
        this.structureTags = new HashSet<>(Arrays.asList(structureTags));
        this.regex = regex;
    }

    StructureTag(String tag, String[] structureTags) {
        this(tag, structureTags, null);
    }
    
    public static String getTags(ParsedRelationStructure relation) {
        Set<String> set = new LinkedHashSet<>();
        set.add(STRUCTURE_TAG.tag);
        for (StructureTag tag : StructureTag.values()) {
            if (tag.structureTags.contains(relation.getChild()) || tag.structureTags.contains(relation.getParent()) ||
                    (tag.regex != null && (relation.getChild().matches(tag.regex) || relation.getParent().matches(tag.regex)))) {
                set.add(tag.tag);
            }
        }
        return String.join(",", set);
    }
    
    public String getTag() {
        return tag;
    }
}
