<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version='1.0'>

    <!-- docbook stylesheet customizations for openjpa manual -->
	<!-- <xsl:import href="http://docbook.sourceforge.net/release/xsl/1.69.1/html/docbook.xsl"/> -->
    <!-- locally downloaded cache of stylesheets -->
	<xsl:import href="../../../target/stylesheets/1.69.1/html/docbook.xsl"/>

	<xsl:param name="html.stylesheet">css/docbook.css</xsl:param>

	<xsl:param name="html.cleanup" select="1"/>
	<xsl:param name="label.from.part" select="1"/>
	<xsl:param name="annotate.toc" select="1"/>
	<xsl:param name="toc.section.depth">5</xsl:param>
	<xsl:param name="generate.section.toc.level" select="8"/>
	<xsl:param name="generate.id.attributes" select="1"/>
	<xsl:param name="generate.index" select="1"/>
	<xsl:param name="chapter.autolabel" select="1"/>
	<xsl:param name="appendix.autolabel" select="1"/>
	<xsl:param name="part.autolabel" select="1"/>
	<xsl:param name="preface.autolabel" select="0"/>
	<xsl:param name="qandadiv.autolabel" select="1"/>
	<xsl:param name="section.autolabel" select="1"/>
	<xsl:template name="process.image.attributes"/>
</xsl:stylesheet>

