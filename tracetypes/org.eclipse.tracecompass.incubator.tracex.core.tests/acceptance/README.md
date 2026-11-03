# TraceX acceptance reproduction

The JSON summaries describe input records, not events synthesized by a viewer.
The schema retains byte order, complete valid-object registry, first and last
logical records, event histogram, every raw/masked/relative timestamp, selected
aliases, and a SHA-256 digest of all logical records. Integers are unsigned input
words unless they represent relative ticks. Physical time remains unknown.
`golden/` comes from the legacy TML library; `summaries/` comes from TraceX core.
`differential.json` preserves the legacy UDP insertion instead of treating it as
an input event. Schema validation also requires timestamp-array lengths and
histogram totals to equal `event_count`; these cross-field constraints are checked
separately from JSON Schema.

The samples are the four binary demos in the [pinned upstream TraceX
source][samples]. Obtain them directly from that source; this repository contains
derived summaries and generated fixtures rather than copies of those binaries.
Each summary records its input hash. The upstream project supplies an MIT license;
the capture adapter preserves the legacy source's original copyright and license.

## Decoder and golden comparison

Use JDK 21 and an extracted built Trace Server product. Its `plugins` directory
provides the already-built TraceX core and Gson jars. The exporter is an optional
acceptance utility, outside the Maven source folders. It does not add product
dependencies. From the repository root, set `ACCEPTANCE`, `SERVER`, `OUT`, and
`TRACEX_SOURCE` to the acceptance directory, extracted product, output directory,
and an unchanged checkout of the pinned TraceX revision, respectively:

```sh
ACCEPTANCE=tracetypes/org.eclipse.tracecompass.incubator.tracex.core.tests/acceptance
mkdir -p "$OUT"
javac -cp "$SERVER/plugins/*" -d "$OUT" "$ACCEPTANCE/SummaryExporter.java"
TRACE="$TRACEX_SOURCE/src/MsixPackageProject/TraceFiles/demo_threadx.trx"
java -cp "$OUT:$SERVER/plugins/*" SummaryExporter "$TRACE" decreasing > "$OUT/current.json"
python3 "$ACCEPTANCE/capture_legacy.py" "$TRACEX_SOURCE" "$TRACE" > "$OUT/legacy.json"
python3 "$ACCEPTANCE/compare.py" "$OUT/legacy.json" "$OUT/current.json"
python3 "$ACCEPTANCE/summarize.py" "$TRACE" "$OUT/current.json" > "$OUT/summary.json"
python3 "$ACCEPTANCE/summarize_legacy.py" "$TRACE" "$OUT/legacy.json" decreasing > "$OUT/golden.json"
```

Repeat for FileX, TCP, and UDP with `increasing`. The legacy capture requires
Python 3 and a C++14 compiler (`g++`). It checks the source revision and both
library-file hashes, replaces Windows integer widths for an LP64 host, removes
managed UI includes, and adds observation fields for raw timer words and
synthetic-record provenance. It leaves parsing, rotation, direction inference,
and synthetic-event insertion intact. Temporary copies keep the original MIT
headers. Synthetic events have no input raw words, so those undefined legacy
fields are omitted. The comparator returns a nonzero status whenever it reports
a difference, including the known UDP insertion. It does not silently accept it.

Compare complete exports field by field before relying on the digests. Registry
names, addresses, types, parameters, contexts, raw IDs/priorities/information
words, raw timestamps, masked timestamps, relative ticks, and endpoints match all
3,824 original records. Independently selected alias contracts use the
[ThreadX][threadx], [FileX][filex], and [NetX Duo][netx] headers. Golden summaries
contain one selected contract per demo; current summaries additionally list the
first example of every event type. Compare each golden selected field with its
matching current event, not the two selection lists as a whole.

## Endian and server fixtures

```sh
python3 "$ACCEPTANCE/generate_fixtures.py" "$OUT/fixtures"
java -cp "$OUT:$SERVER/plugins/*" SummaryExporter "$OUT/fixtures/little-execution.trx" increasing > "$OUT/little.json"
java -cp "$OUT:$SERVER/plugins/*" SummaryExporter "$OUT/fixtures/big-execution.trx" increasing > "$OUT/big.json"
```

The complete models must match after excluding only the `byte_order` discriminator.
Both fixtures have seven events and a thread named `worker [0x20001000]`. Query
execution at ticks 0 through 6: Initialization, worker, ISR 7, ISR 8, ISR 7,
worker, Idle. The state response ends at tick 7 because interval ends are exclusive.
The direct analysis tests verify the same transition semantics; server evidence
records this exact generated fixture independently. `server_check.py` runs the
REST workflow for the four demos and the generated fixtures. Use a separate
server workspace and pass a directory containing only those inputs:

```sh
python3 "$ACCEPTANCE/server_check.py" --base-url http://localhost:8080/tsp/api "$OUT/fixtures" "$OUT/server-evidence"
```

Run it again with the demo directory. The utility discovers the shared clock
configuration source, applies increasing direction to FileX/TCP/UDP and decreasing
direction to ThreadX before opening experiments, verifies their endpoints, and
closes each experiment after capture. All frequencies remain unknown. The
ThreadX endpoint is 156206 ordering ticks after configuration.

Exercise known frequency, clearing it, rejected active/invalid edits, reset,
and reloaded provider states with the same generated fixture:

```sh
python3 "$ACCEPTANCE/clock_check.py" --base-url http://localhost:8080/tsp/api "$OUT/fixtures/little-execution.trx" "$OUT/clock-evidence"
```

Both utilities retain portable request/response evidence and fail on unexpected
status, event count, endpoint, or fixture-state results. Use distinct fixture
locations if a server workspace already imports an input under another name;
clock configuration rejects ambiguous imported locations.

For desktop checks, follow the installed user guide. Capture installation,
restart, import, event counts and endpoints, registry fields, chronological ordering, searching,
filtering, table/view selection, field inspection, and nested-ISR screenshots.
Read `ACCEPTANCE.md` before claiming a complete acceptance pass.

[samples]: https://github.com/eclipse-threadx/tracex/tree/b1af4decb38d48958ce3fa5c70d403f0d48aa25b/src/MsixPackageProject/TraceFiles
[threadx]: https://github.com/eclipse-threadx/threadx/blob/e73752681bd405deddf247d1cf2b899d502dceaa/common/inc/tx_trace.h
[filex]: https://github.com/eclipse-threadx/filex/blob/cdc7a593d0555cdfcfb6181341daad2df1524b23/common/inc/fx_api.h
[netx]: https://github.com/eclipse-threadx/netxduo/blob/20046b12214490541d3090ff840bec4b78e67382/common/inc/nx_api.h

The supplied release has no arbitrary column-sort action. Chronological ordering is accepted by the maintainer; the limitation remains
documented. Installed Help launch is verified with a configured external browser.
Native Windows passed with the retained Statistics cleanup diagnostic; see
`windows.json` and `windows/RESULTS.md`. Native macOS remains pending in
`ACCEPTANCE.md`. `WINDOWS.md` retains the Windows reproduction procedure.

The stock-view follow-up has separate evidence in `STOCK-VIEWS.md` and
`stock-views-verification.json`, including its test reuse, display setup, final
products, and pending native platform checks.
