<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="text"/>

    <xsl:param name="version"/>
    <xsl:param name="date"/>

    <xsl:template match="/">
        <xsl:call-template name="TC000"/>
    </xsl:template>

    <xsl:template name="substring-after-last">
        <xsl:param name="string"/>

        <xsl:choose>
            <xsl:when test="contains($string, '\')">
                <xsl:call-template name="substring-after-last">
                    <xsl:with-param name="string" select="substring-after($string, '\')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="contains($string, '/')">
                <xsl:call-template name="substring-after-last">
                    <xsl:with-param name="string" select="substring-after($string, '/')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$string"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="output">
        <xsl:param name="fileName"/>
        <xsl:param name="class"/>
        <xsl:value-of
                select="concat($class, '&#x9;Q0&#x9;')"/>
        <xsl:call-template name="substring-after-last">
            <xsl:with-param name="string" select="$fileName"/>
        </xsl:call-template>
        <xsl:value-of select="concat('&#x9;0&#x9;1&#x9;', $date, '_veraPDF_', $version, '&#xa;')"/>
    </xsl:template>

    <xsl:template name="TC000">
        <xsl:param name="class" select="'TC000'"/>
        <xsl:for-each select="/report/jobs/job">
            <xsl:if test="validationReport/@isCompliant = 'true'">
                <xsl:variable name="fileName" select="item/name"/>
                <xsl:call-template name="output">
                    <xsl:with-param name="fileName" select="$fileName"/>
                    <xsl:with-param name="class" select="$class"/>
                </xsl:call-template>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>