package org.verapdf.tools;

public enum StructureType {
    DOCUMENT("Document"),
    PART("Part"),
    DIV("Div"),
    CAPTION("Caption"),
    THEAD("THead"),
    TBODY("TBody"),
    TFOOT("TFoot"),
    H("H"),
    P("P"),
    L("L"),
    LI("LI"),
    LBL("Lbl"),
    LBODY("LBody"),
    TABLE("Table"),
    TR("TR"),
    TH("TH"),
    TD("TD"),
    SPAN("Span"),
    LINK("Link"),
    ANNOT("Annot"),
    RUBY("Ruby"),
    WARICHU("Warichu"),
    FIGURE("Figure"),
    FORMULA("Formula"),
    FORM("Form"),
    RB("RB"),
    RT("RT"),
    RP("RP"),
    WT("WT"),
    WP("WP"),
    ART("Art"),
    SECT("Sect"),
    BLOCK_QUOTE("BlockQuote"),
    TOC("TOC"),
    TOCI("TOCI"),
    INDEX("Index"),
    NON_STRUCT("NonStruct"),
    PRIVATE("Private"),
    QUOTE("Quote"),
    NOTE("Note"),
    REFERENCE("Reference"),
    BIB_ENTRY("BibEntry"),
    CODE("Code"),
    H1("H1"),
    H2("H2"),
    H3("H3"),
    H4("H4"),
    H5("H5"),
    H6("H6"),
    DOCUMENT_FRAGMENT("DocumentFragment"),
    ASIDE("Aside"),
    TITLE("Title"),
    FENOTE("FENote"),
    SUB("Sub"),
    EM("Em"),
    STRONG("Strong"),
    ARTIFACT("Artifact");

    private final String text;
    private StructureType(String text) {
        this.text = text;
    }

    public String string() {
        return text;
    }
}
