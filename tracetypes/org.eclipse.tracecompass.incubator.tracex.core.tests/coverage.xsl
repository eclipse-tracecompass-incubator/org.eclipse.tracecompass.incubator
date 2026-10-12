<?xml version="1.0" encoding="UTF-8"?>
<!--
   Copyright (c) 2026 Eclipse Trace Compass Incubator contributors

   All rights reserved. This program and the accompanying materials
   are made available under the terms of the Eclipse Public License 2.0
   which accompanies this distribution, and is available at
   https://www.eclipse.org/legal/epl-2.0/

   SPDX-License-Identifier: EPL-2.0
-->
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" encoding="UTF-8" />

  <xsl:template match="/">
    <xsl:variable name="scope" select="//class[
        (starts-with(@name, 'org/eclipse/tracecompass/incubator/internal/tracex/core/trace/')
          and not(starts-with(@name, 'org/eclipse/tracecompass/incubator/internal/tracex/core/trace/TraceXTrace')))
        or @name = 'org/eclipse/tracecompass/incubator/internal/tracex/core/analysis/TraceXAnalysis'
        or starts-with(@name, 'org/eclipse/tracecompass/incubator/internal/tracex/core/analysis/TraceXExecutionContext')
        or @name = 'org/eclipse/tracecompass/incubator/internal/tracex/core/analysis/TraceXStateProvider'
        ]" />
    <xsl:text>tracex.class.count=</xsl:text>
    <xsl:value-of select="count($scope)" />
    <xsl:text>&#10;</xsl:text>
    <xsl:text>tracex.line.missed=</xsl:text>
    <xsl:value-of select="sum($scope/counter[@type = 'LINE']/@missed)" />
    <xsl:text>&#10;tracex.branch.missed=</xsl:text>
    <xsl:value-of select="sum($scope/counter[@type = 'BRANCH']/@missed)" />
    <xsl:text>&#10;</xsl:text>
  </xsl:template>
</xsl:stylesheet>
