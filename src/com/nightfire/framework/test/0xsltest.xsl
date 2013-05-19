<?xml version="1.0"?>

<!-- Root rule -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/TR/WD-xsl"
	result-ns=""
	default-space="preserve">

<xsl:template match="/">
<xsl:pi name="xml">version="1.0" standalone="yes"</xsl:pi>
<RESPONSE>
	<xsl:apply-templates/>
</RESPONSE>
</xsl:template>

<!-- BAK Segement Rules -->

<xsl:template match="SEGMENT[@ID='BAK']">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="DATAELEMENT[@ID='BAK01' and @value='06']">
	<RESPONSE_TYPE value='CONFIRMED'/>
</xsl:template>

<xsl:template match="DATAELEMENT[@ID='BAK01' and @value='44']">
	<RESPONSE_TYPE value='REJECTED'/>
</xsl:template>

<xsl:template match="DATAELEMENT[@ID='BAK01' and @value='CN']">
	<RESPONSE_TYPE value='COMPLETED'/>
</xsl:template>

<xsl:template match="DATAELEMENT[@ID='BAK01' and @value='01']">
	<RESPONSE_TYPE value='COMPLETED'/>
</xsl:template>

<xsl:template match="DATAELEMENT[@ID='BAK03']">
	<PON value="{@value}"/>
</xsl:template>



<!-- REF Segement Rules -->

<xsl:template match="SEGMENT[@ID='REF' and 
		     DATAELEMENT[@value='2I'] and
		     DATAELEMENT[@value='LSRNO']]">
	<xsl:for-each select="DATAELEMENT[@ID='REF02']">
	<LSRNO value="{@value}"/>
	</xsl:for-each>
</xsl:template>

<xsl:template match="LOOP[@ID='PO1']/SEGMENT[@ID='SI' and
		     DATAELEMENT[@ID='SI01' and @value='TI'] and
		     DATAELEMENT[@ID='SI02' and @value='CM']]">
	<xsl:for-each select="DATAELEMENT[@ID='SI03']">
	<ECCKT value="{@value}"/>
	</xsl:for-each>
</xsl:template>

<!-- Default rule -->


</xsl:stylesheet>
