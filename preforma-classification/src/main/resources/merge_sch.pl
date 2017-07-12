#!/usr/bin/perl -w
use File::Find;

$num_args = $#ARGV + 1;
if ($num_args != 2) {
    print "\nUsage: merge_sch.pl <Policy dir> <output stylesheet>\n";
    exit;
}
$policy_dir = $ARGV[0];
$stylesheet = $ARGV[1];


my $part1 = << 'END_PART1';
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:x="adobe:ns:meta/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">

    <xsl:output method="text"/>

    <xsl:param name="version" select="/report/buildInformation/releaseDetails[@id='gui']/@version"/>
    <xsl:param name="date"/>
    <xsl:param name="onlyPassed"/>
END_PART1

my $part2 = q{};
open my ($fh2), '>', \$part2;

my $part3 = << 'END_PART3';
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
END_PART3

my $part4 = q{};
open my ($fh4), '>', \$part4;

print $fh2 "    <xsl:template match=\"/\">\n";

find(\&process, $policy_dir);

print $fh2 "    </xsl:template>\n\n";
print $fh4 "</xsl:stylesheet>\n";

close $fh2;
close $fh4;

# final output
open my($fh), '>', $stylesheet;
print $fh $part1;
print $fh $part2;
print $fh $part3;
print $fh $part4;
close $fh;

# processing each policy file
sub process
{
	return unless (/^(.*)\.sch$/);
	$tc = $1;
	
	print $fh2 "        <xsl:call-template name=\"$tc\"/>\n";

	print $fh4 "    <xsl:template name=\"$tc\">\n";
    print $fh4 "        <xsl:param name=\"class\" select=\"\'$tc\'\"/>\n";
    print $fh4 "        <xsl:for-each select=\"/report/jobs/job\">\n";
    print $fh4 "        <xsl:variable name=\"fileName\" select=\"item/name\"/>\n";
    print $fh4 "        <xsl:variable name=\"score\">\n";
    print $fh4 "            <xsl:choose>\n";
	
	open(SCH,$_);
	while(<SCH>) 
	{
		if(/^\s*<sch:assert test="not\((.*)\).*$/) 
		{ 
			print $fh4 "               <xsl:when test=\"$1\">1</xsl:when>\n"; 
		}
	}
	
    print $fh4 "               <xsl:otherwise>0</xsl:otherwise>\n";
	print $fh4 "            </xsl:choose>\n";
	print $fh4 "        </xsl:variable>\n";
    print $fh4 "        <xsl:call-template name=\"output\">\n";
    print $fh4 "            <xsl:with-param name=\"fileName\" select=\"\$fileName\"/>\n";
    print $fh4 "            <xsl:with-param name=\"class\" select=\"\$class\"/>\n";
    print $fh4 "            <xsl:with-param name=\"score\" select=\"\$score\"/>\n";
    print $fh4 "        </xsl:call-template>\n";
    print $fh4 "        </xsl:for-each>\n";
    print $fh4 "    </xsl:template>\n";
}


