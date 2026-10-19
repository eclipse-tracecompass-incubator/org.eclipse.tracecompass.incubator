# Native Windows TraceX acceptance

The run at `b65e0a8d` passed on 2026-10-14 through 2026-10-15 with retained
Statistics cleanup diagnostics. See [the results](windows/RESULTS.md) and
[the portable record](windows.json). This procedure remains available for
reproduction; do not repeat a verified run for evidence-only documentation edits.

Run this procedure in Windows PowerShell and native Windows applications. It
checks Windows launch, p2 installation, file handling, widgets, clock persistence,
help, and the server. Linux/WSL results do not substitute for these checks.
Allow about 45–60 minutes. Keep the raw evidence private until it is reviewed.

## Inputs and prerequisites

- Windows x86_64, Git for Windows, Python 3, and a working Windows browser.
- The `tracex-feature-integration` branch from the maintainer's fork.
- The verified Windows desktop ZIP, Windows server ZIP, and p2 ZIP supplied with
  the test handoff. A Git clone does **not** contain these build outputs.
- Network access to obtain the matching clean Trace Compass release and demos.

The verified parser/UI/server source is `8271366bd301bf3e4a86c69b1efe830fdf8d2a3a`.
The supplied artifacts include documentation-only Help fixes from
`03ca59016aeee2f4913b047dffe96676934b8e86`.
Documentation and acceptance-only commits may follow it on the branch.
The supplied Windows products include their Java runtime; a separate JDK/Maven
installation is not required to test these binaries. Do not rebuild merely to
perform this smoke test.

| Archive | SHA-256 |
| --- | --- |
| `trace-compass-0.19.0-20261013-2127-win32.win32.x86_64.zip` | `4fe46f8b1f5fa9926d5227804b554171c189f6d10489c538304342f06b6cfc3b` |
| `trace-compass-server0.19.0-20261013-2127-win32.win32.x86_64.zip` | `7de987076ef9385a949b351eb631b2892ba1ab3a92772497dbc2bd4829bbd50d` |
| `tracex-p2-0.19.0.202610132127.zip` | `ce9f9f71d1635059aec938d2b4d3e7d25c4475b621ba81a7648018d707e5306d` |

Use an ordinary user account. Create fresh workspaces; do not reuse a production
workspace. The paths below are chosen for this test and may be changed. Keep the
PowerShell session open so the variables remain defined.

```powershell
$ErrorActionPreference = 'Stop'
$testRoot = Join-Path $env:LOCALAPPDATA 'TraceXAcceptance'
$downloads = Join-Path $testRoot 'downloads'
$evidence = Join-Path $testRoot 'evidence'
New-Item -ItemType Directory -Force $downloads, $evidence | Out-Null
$forkUrl = Read-Host 'HTTPS clone URL of the maintainer fork'
$repo = Join-Path $testRoot 'source'
git clone --branch tracex-feature-integration --single-branch $forkUrl $repo
if ($LASTEXITCODE -ne 0) { throw 'Clone failed' }
git -C $repo merge-base --is-ancestor 8271366bd301bf3e4a86c69b1efe830fdf8d2a3a HEAD
if ($LASTEXITCODE -ne 0) { throw 'Branch does not contain the verified implementation' }
git -C $repo rev-parse HEAD | Set-Content (Join-Path $evidence 'source-commit.txt')
$acceptance = Join-Path $repo 'tracetypes\org.eclipse.tracecompass.incubator.tracex.core.tests\acceptance'
py -3 --version
if ($LASTEXITCODE -ne 0) { throw 'Python 3 is required for fixture/server utilities' }
```

Copy the three supplied archives into `$downloads`. Check each hash with
`Get-FileHash -Algorithm SHA256`; it must match the table. Save the results:

```powershell
Get-ChildItem $downloads -Filter '*.zip' |
    Get-FileHash -Algorithm SHA256 |
    Format-List | Out-File (Join-Path $evidence 'archive-hashes.txt')
```

Do not use a partial download or a different build after a hash mismatch.

## Prepare native products and test inputs

Download the clean Windows counterpart of the supplied Linux release. The URL
below was checked to return HTTP 200. Record its hash; no reference hash for this
clean stock ZIP is asserted by this procedure.

```powershell
$stockName = 'trace-compass-12.1.0-20260909-1515-win32.win32.x86_64.zip'
$stockZip = Join-Path $downloads $stockName
Invoke-WebRequest -UseBasicParsing -Uri ('https://download.eclipse.org/tracecompass/releases/12.1.0/rcp/' + $stockName) -OutFile $stockZip
Get-FileHash $stockZip -Algorithm SHA256 |
    Format-List | Out-File (Join-Path $evidence 'stock-release-hash.txt')
Expand-Archive $stockZip (Join-Path $testRoot 'stock')
Expand-Archive (Join-Path $downloads 'trace-compass-0.19.0-20261013-2127-win32.win32.x86_64.zip') (Join-Path $testRoot 'incubator')
Expand-Archive (Join-Path $downloads 'trace-compass-server0.19.0-20261013-2127-win32.win32.x86_64.zip') (Join-Path $testRoot 'server')
Expand-Archive (Join-Path $downloads 'tracex-p2-0.19.0.202610132127.zip') (Join-Path $testRoot 'p2')
$stockExe = (Get-ChildItem (Join-Path $testRoot 'stock') -Recurse -Filter 'tracecompass.exe' | Select-Object -First 1).FullName
$desktopExe = (Get-ChildItem (Join-Path $testRoot 'incubator') -Recurse -Filter 'tracecompass.exe' | Select-Object -First 1).FullName
$serverExe = (Get-ChildItem (Join-Path $testRoot 'server') -Recurse -Filter 'tracecompass-serverc.exe' | Select-Object -First 1).FullName
if (!$stockExe -or !$desktopExe -or !$serverExe) { throw 'Expected Windows executable missing' }
$inputs = Join-Path $testRoot 'inputs'
New-Item -ItemType Directory -Force $inputs | Out-Null
$sampleRevision = 'b1af4decb38d48958ce3fa5c70d403f0d48aa25b'
foreach ($name in 'demo_filex','demo_netx_tcp','demo_netx_udp','demo_threadx') {
    $summary = Get-Content (Join-Path $acceptance ('summaries\' + $name + '.json')) -Raw | ConvertFrom-Json
    $sample = Join-Path $inputs ($name + '.trx')
    $url = 'https://raw.githubusercontent.com/eclipse-threadx/tracex/' + $sampleRevision + '/src/MsixPackageProject/TraceFiles/' + $name + '.trx'
    Invoke-WebRequest -UseBasicParsing $url -OutFile $sample
    if ((Get-FileHash $sample -Algorithm SHA256).Hash.ToLowerInvariant() -ne $summary.input_sha256) {
        throw ('Demo hash mismatch: ' + $name)
    }
}
py -3 (Join-Path $acceptance 'generate_fixtures.py') $inputs
if ($LASTEXITCODE -ne 0) { throw 'Fixture generation failed' }
```

If a directory already exists, choose a new test root rather than overwriting an
earlier run. ZIP extraction must retain the executable, plugins, configuration,
and bundled runtime together.

## Clean-release feature installation

Launch the stock executable with a fresh workspace:

```powershell
$stockWorkspace = Join-Path $testRoot 'stock-workspace'
& $stockExe -data $stockWorkspace -consoleLog
```

1. Select **Help > Install New Software > Add > Local** and choose `$testRoot\p2`,
   the directory containing `content.jar` and `artifacts.jar`. If using **Archive**,
   choose the p2 ZIP itself.
2. Select **Trace Types > Trace Compass TraceX (Incubation)**. Complete installation
   and restart when prompted. Record any dependency or certificate prompt.
3. In **Help > About > Installation Details**, confirm the TraceX feature version
   `0.19.0.202610132127`; core/UI bundles are `0.1.0.202610132127` and documentation
   is `0.19.0.202610132127`.
4. Capture `installation.png`. Failure to install into the clean release is an
   acceptance failure even if the bundled Incubator desktop launches.

Perform the following desktop checks in this installed stock application.

## Desktop checks and expected results

Import all six inputs through **File > Import > Tracing > Traces**, or the direct
Trace Import dialog if shown. Let it create a tracing project. Open each imported
trace. If necessary, select **Select Trace Type > TraceX > TraceX Trace** on the
resource. Wait for indexing and analysis jobs to finish before checking results.

| Trace | Events | Registry objects | Event types | Direction | Final relative tick |
| --- | ---: | ---: | ---: | --- | ---: |
| FileX | 950 | 6 | 25 | increasing | 949000 |
| NetX TCP | 950 | 18 | 36 | increasing | 949000 |
| NetX UDP | 950 | 16 | 23 | increasing | 949000 |
| ThreadX | 974 | 15 | 11 | decreasing | 156206 |
| Each execution fixture | 7 | 1 | 4 | increasing | 6 |

All demo clock frequencies are **unknown**. Explicitly set ThreadX to
**Decreasing** through **TraceX Execution > Clock...** before checking its endpoint.
The default increasing direction instead produces 63610322 ticks for that demo.

For every demo:

1. Confirm event count in **Statistics** and trace metadata in **Properties** after
   selecting the imported resource. Check byte order and registry counts. Do not
   interpret the generic widget's date/time format as physical time.
2. In **TraceX Execution**, inspect named thread rows and their hexadecimal
   addresses. Compare them and event Contents aliases with the summary's registry
   and `selected_fields`. Some objects are resolved in event fields rather than
   displayed as execution rows.
3. Select the first event and use **Ctrl+End** in the Events table to reach the last.
   Confirm `relative_ticks` in Contents; `logical_rank` runs from zero to count−1.
4. Inspect the Statistics event-type distribution. Export the complete Events
   table through its context menu **Export To Text...**, keeping all columns.
   Save as `$evidence\demo_filex.csv`, etc. These files are tab-delimited even when
   named `.csv`. Clear filters before exporting and verify the exported count.
5. Capture one screenshot per demo showing Statistics/count, Events, and the
   execution view. Save exports for the metadata validation described below.

For both `little-execution.trx` and `big-execution.trx`:

| Ordering tick | CPU 0 execution |
| ---: | --- |
| 0 | Initialization |
| 1 | worker [0x20001000] |
| 2 | ISR 7 |
| 3 | ISR 8 |
| 4 | ISR 7 |
| 5 | worker [0x20001000] |
| 6 | Idle |

Open **Window > Show View > TraceX > TraceX Execution**; use **Other...** first if
the menu presents it. Collapse the Contexts branch to expose **CPU 0 > Execution**.
Reset the zoom to show the whole trace. State intervals have exclusive ends, so
Idle occupies `[6,7)` while the final event/state-system timestamp is 6.

- Search `TX_TRACE_ISR_ENTER` in the Events table's Event type search cell. Both
  ISR entries must be highlighted. Select a match and confirm timeline selection.
- Use **Add as Filter** on the search row, or **Ctrl+Enter** there. Exactly two
  events remain. Clear the filter using its red X or **Clear Filters** and confirm
  all seven return.
- Select the ISR 8 interval and confirm the event-table selection follows it.
  Inspect raw information words, `isr_number`, and `system_state` in Contents.
- Hover Idle and the exclusive end at tick 7. No error dialog or new TraceX
  `TimeRangeException` may appear. The outside-bound tooltip can have generic
  widget information without a TraceX context.
- Chronological logical-rank ordering is the accepted behavior. Arbitrary column
  sorting is unavailable; no new arbitrary-sort behavior is expected.

Capture `little-execution.png`, `big-execution.png`, `filter.png`, and
`selection-tooltip.png` with legible state names and view labels.

## Standard views and linked clock refresh

Use newly built products when testing the stock-view integration; the earlier
Windows acceptance record predates these changes.

1. Open **Control Flow**, **Resources**, and **CPU Usage** through
   **Window > Show View > Other**. Keep **TraceX Execution** open as well.
2. For an imported demo, confirm named registered threads appear in Control
   Flow, CPU 0 appears in Resources, and CPU Usage contains thread entries.
   Trace-local numeric TIDs are assigned in registry order. They need not match
   target addresses; the original hexadecimal addresses remain in Contents.
3. Import either generated seven-event execution fixture. Resources must show
   ISR 7 enclosing ISR 8 and then returning to the worker. Control Flow must
   show the worker interrupted and then running. CPU Usage excludes ISR, idle,
   initialization, and unresolved-context time from thread totals.
4. Verify that Events still shows the original ThreadX event names and a
   **Contents** column with raw fields and aliases. Export all columns and run
   the existing complete-export comparison below.
5. Pin Control Flow to the fixture. In **TraceX Execution > Clock...**, choose
   Increasing and a known frequency of `2`. Verify all linked views refresh to
   the new time range and Control Flow remains pinned. Clear the known-frequency
   checkbox and confirm they shrink back to the six-tick fixture endpoint.
6. Create an experiment containing two imported TraceX inputs and confirm all
   three standard views show both member traces. Close the experiment before
   changing a member's clock.
7. Capture `stock-control-flow.png`, `stock-resources.png`, `stock-cpu-usage.png`,
   and `stock-clock-refresh.png`. Record any missing rows, stale time ranges,
   unexpected synthetic event names, or workspace log errors in the handoff.

## Clock validation and full process restart

On the little-endian fixture, open **Clock...**:

1. Keep Increasing direction, select **Clock frequency is known**, and enter `0`.
   Expect an explanatory validation error and disabled **OK**. Capture it.
2. Enter `2` and confirm. Expect **Nanoseconds; 2 ticks per second; increasing**,
   seven events, a final table timestamp of 3,000,000,000 ns, and rebuilt execution
   bounds. Reset zoom if needed. Capture `clock-known.png`.
3. Exit the entire application through **File > Exit**, then relaunch the same
   executable with the **same** `$stockWorkspace`. Reopen the fixture and ThreadX.
   Frequency 2 must persist for the fixture; ThreadX must retain Decreasing and
   endpoint 156206. Capture `clock-restarted.png`.
4. Clear **Clock frequency is known** on the fixture and confirm. Expect **Ordering
   ticks; physical time unknown; increasing** and endpoint 6. The event table and
   execution view must both refresh. Capture `clock-cleared.png`.
5. Exit/relaunch once more in the same workspace; unknown frequency and endpoint 6
   must persist. An old three-second range or stale table timestamp is a failure.

## Installed help and bundled desktop launch

Open **Help > Help Contents**. Open the **TraceX** book/topic and read Installation
and import, Clock configuration, Event fields, Execution view, Trace Server, and
Limitations. The guide must render in a Windows browser without missing topics or
resources. Capture `help.png` with TraceX content visible.

If no browser is configured, use **Window > Preferences > General > Web Browser**,
select an external installed browser, apply, and retry. Record the configuration
needed; do not mark Help passed merely because a documentation JAR exists.

Close the stock application. Launch the **bundled Incubator Windows desktop** in
a separate workspace:

```powershell
$productWorkspace = Join-Path $testRoot 'incubator-workspace'
& $desktopExe -data $productWorkspace -consoleLog
```

No feature install is needed there. Import the little-endian fixture, open TraceX Execution, confirm
the seven states, and open the installed TraceX help. This separately checks the
built Windows desktop's native launcher and packaged bundles. Close it afterward.

## Native server checks

Open a second Windows PowerShell window and run:

```powershell
$testRoot = Join-Path $env:LOCALAPPDATA 'TraceXAcceptance'
$serverExe = (Get-ChildItem (Join-Path $testRoot 'server') -Recurse -Filter 'tracecompass-serverc.exe' | Select-Object -First 1).FullName
$serverWorkspace = Join-Path $testRoot 'server-workspace'
& $serverExe -data $serverWorkspace -consoleLog -vmargs '-Dtraceserver.host=127.0.0.1' '-Dtraceserver.port=8094'
```

Leave that process running. In the original PowerShell window:

Use the supplied `tracecompass-serverc.exe` console launcher so console logging
and Ctrl+C work in that window. It uses the same packaged server and runtime as
`tracecompass-server.exe`.

```powershell
$baseUrl = 'http://127.0.0.1:8094/tsp/api'
Invoke-RestMethod ($baseUrl + '/health')
py -3 (Join-Path $acceptance 'server_check.py') --base-url $baseUrl $inputs (Join-Path $evidence 'server')
if ($LASTEXITCODE -ne 0) { throw 'Native server input/provider workflow failed' }
py -3 (Join-Path $acceptance 'clock_check.py') --base-url $baseUrl (Join-Path $inputs 'little-execution.trx') (Join-Path $evidence 'server')
if ($LASTEXITCODE -ne 0) { throw 'Native server clock workflow failed' }
```

Expect health UP; all six imports with the counts/endpoints above; discovery of
`org.eclipse.tracecompass.incubator.tracex.core.dataprovider`; COMPLETED trees and
states; both fixtures' exact seven-state sequence. Clock checks must report
known/unknown bounds, active/invalid rejection, and reset. ThreadX is configured
Decreasing before experiment creation. Keep the request/response JSON.

Stop the server with **Ctrl+C** and restart the identical executable/workspace.
In the original window, verify its retained configuration and reopened provider:

```powershell
$capturedClock = Get-Content (Join-Path $evidence 'server\demo_threadx.clock.json') -Raw | ConvertFrom-Json
$capturedTrace = Get-Content (Join-Path $evidence 'server\demo_threadx.trace.json') -Raw | ConvertFrom-Json
$clockRoute = $baseUrl + '/config/types/org.eclipse.tracecompass.incubator.tracex.core.clock/configs/' + $capturedClock.response.id
$clock = Invoke-RestMethod $clockRoute
if ($clock.parameters.timer_direction -ne 'decreasing' -or $clock.parameters.PSObject.Properties['ticks_per_second']) {
    throw 'ThreadX clock did not survive restart as decreasing/unknown'
}
$request = @{parameters=@{name='windows-restart';traces=@($capturedTrace.response.UUID)}} | ConvertTo-Json -Depth 8
$created = Invoke-RestMethod -Method Post -Uri ($baseUrl + '/experiments') -ContentType 'application/json' -Body $request
$experimentRoute = $baseUrl + '/experiments/' + $created.UUID
for ($i=0; $i -lt 120; $i++) {
    $experiment = Invoke-RestMethod $experimentRoute
    if ($experiment.indexingStatus -eq 'COMPLETED') { break }
    Start-Sleep -Milliseconds 100
}
if ($experiment.indexingStatus -ne 'COMPLETED' -or $experiment.nbEvents -ne 974 -or $experiment.end -ne 156206) {
    throw 'Restarted ThreadX experiment has incorrect count, time, or status'
}
$providerRoute = $experimentRoute + '/outputs/timeGraph/org.eclipse.tracecompass.incubator.tracex.core.dataprovider'
$treeQuery = @{parameters=@{requested_times=@(0,156206)}} | ConvertTo-Json -Depth 8
for ($i=0; $i -lt 120; $i++) {
    $tree = Invoke-RestMethod -Method Post -Uri ($providerRoute + '/tree') -ContentType 'application/json' -Body $treeQuery
    if ($tree.status -eq 'COMPLETED') { break }
    Start-Sleep -Milliseconds 100
}
if ($tree.status -ne 'COMPLETED') { throw 'Restarted execution tree did not complete' }
$cpu = $tree.model.entries | Where-Object { $_.labels.Count -eq 1 -and $_.labels[0] -eq 'Execution' } | Select-Object -First 1
if (!$cpu) { throw 'Execution row missing' }
$stateQuery = @{parameters=@{requested_times=@(0,10000,100000,156206);requested_items=@($cpu.id)}} | ConvertTo-Json -Depth 8
$states = Invoke-RestMethod -Method Post -Uri ($providerRoute + '/states') -ContentType 'application/json' -Body $stateQuery
if ($states.status -ne 'COMPLETED' -or @($states.model.rows).Count -eq 0) { throw 'Restarted execution states missing' }
Invoke-RestMethod -Method Delete $experimentRoute | Out-Null
@{clock=$clock;experiment=$experiment;tree=$tree;states=$states} |
    ConvertTo-Json -Depth 30 | Set-Content -Encoding UTF8 (Join-Path $evidence 'server\restart.json')
```

If a check throws, retain its error and close the created experiment before
retrying. The restart JSON is a private diagnostic capture; review any trace paths
before publication. Stop the server once evidence capture is complete.

The server may log unrelated binary-ftrace detector errors while identifying
TraceX files. Record them separately; TraceX parse/analysis failures, missing
provider responses, or wrong endpoints are acceptance failures.

## Validate complete desktop exports

Run the supplied comparison utility:

```powershell
& (Join-Path $acceptance 'check_windows_exports.ps1') -Acceptance $acceptance -Evidence $evidence
```

If a local execution policy prevents this downloaded script from running, invoke
it in a separate process with `powershell.exe -NoProfile -ExecutionPolicy Bypass
-File <script> -Acceptance <acceptance-directory> -Evidence <evidence-directory>`.
This changes the policy for that process only. The utility verifies every raw
timestamp and relative tick, complete
event-ID histograms, logical ranks, and all retained selected aliases against
the pinned summaries. Expect **3,824 records validated**. Keep its JSON report.
It is an evidence comparison, not an implementation of the parser.

## Evidence handoff and pass criteria

Create `windows-result.md` recording tester, date, Windows version/architecture,
source commit, artifact hashes, feature version, workspace type, and pass/fail
for installation, both native launchers, all demos, endian fixtures, search,
filter/clear, cross-selection, tooltip boundaries, clock/restart, Help, and server
restart. Record a failure's exact action, actual result, and expected result.

Include the named screenshots, full desktop exports, comparison JSON, and portable
server responses. Keep workspace `.metadata\.log` files and server console logs
privately for diagnosis. Review/redact paths and unrelated desktop content before
sharing screenshots or raw logs publicly. Keep failures visible; do not replace
them with a successful run from another platform.

Windows is passed only when every required workflow above succeeds in native
Windows processes and the evidence is retained. Building Windows ZIPs or cloning
the branch alone is insufficient. macOS remains pending and requires a Mac host.
