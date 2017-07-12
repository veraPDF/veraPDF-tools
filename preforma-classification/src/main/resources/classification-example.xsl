<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="text"/>

    <xsl:param name="version" select="/report/buildInformation/releaseDetails[@id='gui']/@version"/>
    <xsl:param name="date"/>
    <xsl:param name="onlyPassed"/>

    <xsl:template match="/">
        <xsl:call-template name="TC000"/>
        <xsl:call-template name="TC010"/>
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
        <xsl:param name="score"/>
        <xsl:if test="$onlyPassed = 'false' or not($score = 0)">
            <xsl:value-of select="concat($class, '&#x9;Q0&#x9;')"/>
            <xsl:call-template name="substring-after-last">
                <xsl:with-param name="string" select="$fileName"/>
            </xsl:call-template>
            <xsl:value-of select="concat('&#x9;0&#x9;', $score, '&#x9;', $date, '_veraPDF_', $version, '&#xa;')"/>
        </xsl:if>
    </xsl:template>

    <xsl:template name="TC000">
        <xsl:param name="class" select="'TC000'"/>
        <xsl:for-each select="/report/jobs/job">
            <xsl:variable name="fileName" select="item/name"/>
            <xsl:variable name="score">
                <xsl:choose>
                    <xsl:when test="validationReport/@isCompliant = 'true'">1</xsl:when>
                    <xsl:otherwise>0</xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:call-template name="output">
                <xsl:with-param name="fileName" select="$fileName"/>
                <xsl:with-param name="class" select="$class"/>
                <xsl:with-param name="score" select="$score"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="TC010">
        <xsl:param name="class" select="'TC010'"/>
        <xsl:for-each select="/report/jobs/job">
            <xsl:variable name="fileName" select="item/name"/>
            <xsl:variable name="score">
                <xsl:choose>
                    <xsl:when test="validationReport/details/rule[@specification='ISO 19005-1:2005' and @clause='6.2.4' and @testNumber='3']">1</xsl:when>
                    <xsl:when test="validationReport/details/rule[@specification='ISO 19005-2:2011' and @clause='6.2.8' and @testNumber='3']">1</xsl:when>
                    <xsl:when test="validationReport/details/rule[@specification='ISO 19005-3:2012' and @clause='6.2.8' and @testNumber='3']">1</xsl:when>
                    <xsl:otherwise>0</xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:call-template name="output">
                <xsl:with-param name="fileName" select="$fileName"/>
                <xsl:with-param name="class" select="$class"/>
                <xsl:with-param name="score" select="$score"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>