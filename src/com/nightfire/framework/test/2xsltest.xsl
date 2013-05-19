<?xml version="1.0"?>

<!-- A test for xsl:counter -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/TR/WD-xsl" result-ns="">

<!-- The Root Element -->

<xsl:template match="FORM">
<xsl:pi name="xml">version="1.0"</xsl:pi>
<html>
<body bgcolor="#DDDDDD">
<CENTER><H2> 
<xsl:value-of select="@name"/>
</H2></CENTER>

<FORM METHOD="post" ACTION="/Scripts/ttcgi.exe">
<HR/>

<xsl:apply-templates/>
</FORM>
</body>
</html>
</xsl:template>

<xsl:template match="SECTION">
<TABLE>
<TR>
<TD><FONT FACE="sans-serif, Arial, Helvetica" SIZE="4">
<xsl:value-of select="@name"/>
</FONT>
</TD>
<xsl:apply-templates/>
</TR>
</TABLE>
<HR/>
</xsl:template>


<xsl:template match="FIELD">

<TD><FONT FACE="sans-serif, Arial, Helvetica" SIZE="2">
<xsl:value-of select='NAME'/>
<BR/>
<INPUT type='TEXT' MAXLENGTH="{LENGTH}" SIZE="{LENGTH}" NAME="{NAME}"/>
</FONT>
</TD>

</xsl:template>

<xsl:template match="FIELD[TYPE='Enumerated']">

<TD><FONT FACE="sans-serif, Arial, Helvetica" SIZE="2">
<xsl:value-of select='NAME'/>
<BR/>
<SELECT NAME="{NAME}">
	<xsl:apply-templates select="OPTIONS"/>
</SELECT>
</FONT>
</TD>

</xsl:template>

<xsl:template match="OPTIONS">
	<xsl:for-each select="OPTION">
	<OPTION>
	<xsl:apply-templates/>
	</OPTION>
	</xsl:for-each>
</xsl:template>

</xsl:stylesheet>












