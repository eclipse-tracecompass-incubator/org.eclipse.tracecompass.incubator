# Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
# All rights reserved. This program and the accompanying materials are
# made available under the terms of the Eclipse Public License 2.0.
# SPDX-License-Identifier: EPL-2.0

<# Compare native Events-table exports with the pinned TraceX summaries. #>
param(
    [Parameter(Mandatory=$true)][string]$Acceptance,
    [Parameter(Mandatory=$true)][string]$Evidence
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# Extract the lossless Contents fields from one exported event.
function Read-EventContents([string]$Contents) {
    $fields = @{}
    foreach ($item in ($Contents -split ', ')) {
        $separator = $item.IndexOf('=')
        if ($separator -lt 1) { throw ('Invalid event field: ' + $item) }
        $fields[$item.Substring(0, $separator)] = $item.Substring($separator + 1)
    }
    return $fields
}

$reports = @()
foreach ($name in 'demo_filex','demo_netx_tcp','demo_netx_udp','demo_threadx') {
    $summary = Get-Content (Join-Path $Acceptance ('summaries\' + $name + '.json')) -Raw | ConvertFrom-Json
    $export = Join-Path $Evidence ($name + '.csv')
    $rows = @(Import-Csv -Path $export -Delimiter "`t")
    if ($rows.Count -ne $summary.event_count) { throw ($name + ': wrong exported event count; clear filters and export all events') }
    $histogram = @{}
    $fields = @()
    for ($i = 0; $i -lt $rows.Count; $i++) {
        $event = Read-EventContents $rows[$i].Contents
        foreach ($check in @(
            @('logical_rank', [string]$i),
            @('raw_timestamp', [string]$summary.raw_timestamps[$i]),
            @('relative_ticks', [string]$summary.relative_ticks[$i])
        )) {
            if (!$event.ContainsKey($check[0]) -or $event[$check[0]] -cne $check[1]) {
                throw ($name + ': record ' + $i + ' differs in ' + $check[0])
            }
        }
        $id = $event['event_id']
        if (!$id) { throw ($name + ': missing event_id at record ' + $i) }
        if (!$histogram.ContainsKey($id)) { $histogram[$id] = 0 }
        $histogram[$id]++
        $fields += ,$event
    }
    if ($histogram.Count -ne @($summary.histogram.PSObject.Properties).Count) {
        throw ($name + ': wrong event-type histogram size')
    }
    foreach ($entry in $summary.histogram.PSObject.Properties) {
        if (!$histogram.ContainsKey($entry.Name) -or $histogram[$entry.Name] -ne $entry.Value) {
            throw ($name + ': wrong event count for ID ' + $entry.Name)
        }
    }
    foreach ($selected in $summary.selected_fields) {
        $rank = [int]$selected.rank
        if ($rows[$rank].'Event type' -cne $selected.name) { throw ($name + ': wrong selected event type at ' + $rank) }
        foreach ($field in $selected.fields.PSObject.Properties) {
            if (!$fields[$rank].ContainsKey($field.Name) -or $fields[$rank][$field.Name] -cne [string]$field.Value) {
                throw ($name + ': selected field ' + $field.Name + ' differs at rank ' + $rank)
            }
        }
    }
    $reports += [PSCustomObject]@{
        sample=$name
        events=$rows.Count
        eventTypes=$histogram.Count
        selectedContracts=@($summary.selected_fields).Count
        firstTick=$summary.relative_ticks[0]
        lastTick=$summary.relative_ticks[-1]
        exportSha256=(Get-FileHash $export -Algorithm SHA256).Hash.ToLowerInvariant()
        status='passed'
    }
}
$total = ($reports | Measure-Object -Property events -Sum).Sum
if ($total -ne 3824) { throw 'Wrong complete demo record total' }
$reports | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 (Join-Path $Evidence 'windows-desktop-comparison.json')
Write-Output ($total.ToString() + ' records validated: raw timestamps, relative ticks, ranks, complete histograms, and selected aliases')
