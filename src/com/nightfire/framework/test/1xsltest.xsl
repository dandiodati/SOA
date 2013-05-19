<?xml version="1.0"?>

<!-- LSR EDI generation rules-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/TR/WD-xsl"
          result-ns="">

<!-- Here's the rule -->

<xsl:template match="ORDER[@REQUEST_TYPE='New']">
Start the EDI
	<xsl:apply-templates/>
</xsl:template>


<xsl:template match="LSR[ORDER_TYPE[@value='SS']]">
Start the LSR SS header
	<xsl:for-each select="*">
	NODE <xsl:value-of select="@value"/>
	</xsl:for-each>
</xsl:template>

<xsl:template match="EU">
Start the EU form 
	<xsl:for-each select="*">
	NODE <xsl:value-of select="@value"/>
	</xsl:for-each>
</xsl:template>

<xsl:template match="LOOPS">
Start the loops
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="LOOPS">
Start the loops
	<xsl:for-each select="LOOP">
	LOOP
	<xsl:apply-templates/>
	</xsl:for-each>
</xsl:template>

<xsl:template match="LOOP">
Start loop <xsl:value-of select="@ref"/>
	<xsl:for-each select="*">
	Loop Data element <xsl:value-of select="@value"/>
	</xsl:for-each>
</xsl:template>


</xsl:stylesheet>

