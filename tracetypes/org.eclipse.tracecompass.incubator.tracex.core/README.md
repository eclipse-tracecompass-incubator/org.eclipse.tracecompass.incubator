# TraceX binary parsing foundation

The core bundle accepts the 32-bit ThreadX event-trace format documented in
[Chapter 11, Format of event trace buffer][format]. The first four bytes are
matched as raw bytes before a byte order is selected: `54 58 54 42` is big
endian (`TXTB`) and `42 54 58 54` is little endian (`BTXT`). Every pointer and
word is then decoded as an unsigned 32-bit value and represented by a Java
`long`.

A target pointer becomes a file offset only through checked subtraction:

```text
file offset = unsigned target address - unsigned trace base address
```

An address below the base is invalid. All derived additions,
multiplications, and subtractions are checked before a range is used. The
registry starts after the 48-byte control header and ends where the event area
starts. Its entry size is the fixed 16-byte prefix plus the declared name size,
rounded up to the 4-byte alignment used by the documented C structure. The
event area contains one to 32,768 complete 32-byte records, and its current
pointer identifies a record within that half-open range. Both regions must fit
within the actual file.

The public format describes the registry as a C structure but does not state
how a non-word-sized custom object-name setting is padded. Current ThreadX
constructs the region with `sizeof(TX_TRACE_OBJECT_ENTRY)`, so this parser uses
4-byte tail padding. The format also has no version or pointer-width field;
this implementation therefore accepts only the documented 48-byte, 32-bit
layout and does not infer wider variants.

Registry records retain their complete encoded bytes, including unused slots and
stale metadata left for historical event resolution. Event records likewise keep
the raw ID, context, priority, timestamp, and four information words while a
table-driven catalog adds names and typed aliases for the ThreadX, FileX, and
NetX Duo events used by the bundled TraceX demonstrations. Uncataloged USBX,
reserved, user-defined, and unknown IDs remain importable as generic events.

At initialization, the reader scans each physical event slot once. ThreadX
marks an unused slot by clearing its first word, so a partial buffer consists
of the contiguous non-empty prefix before that marker. A non-empty record
after an empty one is rejected as an inconsistent snapshot. When every slot is
occupied, the control header's current pointer identifies the oldest event and
logical rank zero begins there, wrapping once at the physical end. Logical
ordering never depends on timestamps.

Trace locations are logical ranks in the inclusive-start, exclusive-end range
from zero through the event count. Each indexed event retains both that rank
and its physical file offset. Rank and ratio seeks clamp to the same range;
ratio zero selects the first event and ratio one selects the end context.
Reads use positional file access, so separate contexts do not share channel
position or iteration state.

## Time normalization

The raw unsigned timer word is retained on every event. Logical event order is
authoritative: rank zero has relative tick zero, and later ranks accumulate
modular deltas. The timer direction is one trace-wide setting backed by the
target configuration; it is never inferred independently from adjacent event
values. The control header's contiguous low-bit timer mask defines the modular
range. Equal masked values have a zero delta. Increasing timers use
`(current - previous) mod range`; decreasing timers use
`(previous - current) mod range`. Checked arithmetic rejects a trace rather
than allowing accumulated ticks to exceed the signed 64-bit timestamp domain.

Trace Compass timestamps use one ordering unit per relative tick until a clock
frequency is configured. Those values preserve event order but do not claim to
be nanoseconds. A configured frequency is a positive decimal integer no greater
than 1,000,000,000 ticks per second. Nanoseconds are calculated as
`(ticks / frequency) * 1,000,000,000 +
(ticks % frequency) * 1,000,000,000 / frequency`, with checked intermediate
arithmetic. Fractional nanoseconds are rounded down. Frequencies whose converted
trace endpoint cannot fit the timestamp domain are rejected.

Clock settings are stored as persistent properties of an imported trace
resource. Core callers, including headless and server integrations, may set or
inspect the same configuration through `TraceXTrace`; no UI service is needed.
The complete direction/frequency pair uses one atomic resource property, with
backward-compatible reads of older separate properties. Changing a setting
validates and builds the replacement time base before it is committed, then resets the framework index, analysis data, supplementary data,
TraceX data-provider instance, and interested views.

## Execution analysis

The execution analysis builds one CPU timeline plus per-context activity rows.
Thread names and addresses come from the object registry, interrupt rows use the
recorded interrupt number, and initialization, idle, and unresolved contexts
remain explicit. Nested interrupts are maintained as a stack; a scheduler
change during an interrupt becomes visible when the outermost interrupt exits.
An unmatched interrupt exit increments a diagnostic attribute without damaging
the active stack. Interrupt exits pop by nesting order; the recorded interrupt
number is descriptive rather than a correlation key.

The transition table is intentionally limited to events whose information-word
contract identifies the next running context. Other ThreadX service events do
not imply a context switch.

| Event | ID | Next scheduler context | Active CPU context |
| --- | ---: | --- | --- |
| Thread resume | 1 | Information word 4 | Scheduler, unless in an ISR |
| Thread suspend | 2 | Information word 4 | Scheduler, unless in an ISR |
| ISR enter | 3 | Unchanged | Push ISR from information word 2 |
| ISR exit | 4 | Unchanged | Pop innermost ISR, then restore stack/scheduler |
| Time slice | 5 | Information word 1 | Scheduler, unless in an ISR |
| Running | 6 | Event context | Scheduler, unless in an ISR |
| Thread relinquish | 109 | Information word 2 | Scheduler, unless in an ISR |

Events with equal timestamps are processed in trace rank order. Consequently,
the final transition at that timestamp is the state visible to a point query.
The shared data provider exposes this model to both the desktop time-graph view
and headless/server clients, including requested-time sampling, regex filters,
styles, tooltips, selection, zoom, and cancellation.

To extend the catalog, add an entry to `TraceXMetadataCatalog` using the event ID,
symbolic name, component category, and aliases from the corresponding current
component header. An address alias enables registry-name resolution but never
replaces the underlying unsigned information word.

### Demo compatibility and reproducible acceptance

All 3,824 original records across the four bundled demonstrations match the
independent legacy Trace Management Library capture, including unsigned raw
words, masked timestamps, configured relative ticks, endpoints, histograms,
and registry names, addresses, types, and parameters. The
[acceptance directory](../org.eclipse.tracecompass.incubator.tracex.core.tests/acceptance/README.md)
contains source-pinned capture tools, a summary schema, derived golden and current
summaries, and differential reports. The
[acceptance status](../org.eclipse.tracecompass.incubator.tracex.core.tests/acceptance/ACCEPTANCE.md)
records the current desktop, server, platform, and review evidence and gaps.

| Demonstration | Objects | Input events | Event types | Direction | Last relative tick |
| --- | ---: | ---: | ---: | --- | ---: |
| FileX | 6 | 950 | 25 | increasing | 949000 |
| NetX Duo TCP | 18 | 950 | 36 | increasing | 949000 |
| NetX Duo UDP | 16 | 950 | 23 | increasing | 949000 |
| ThreadX | 15 | 974 | 11 | decreasing | 156206 |

The legacy library inserts one synthetic Running event into the UDP demonstration,
so its displayed count is 951. The binary reader returns the 950 input records.
That difference is retained for maintainer review. The ThreadX demo has a 16-bit
mask and a decreasing timer; the default increasing direction does not reproduce
its elapsed tick values. Frequency is unknown for every demo. Interrupt sentinels
and interrupted threads resolve separately from object addresses; UDP contains
27 ISR-context records and ThreadX contains 8. Neither has unresolved contexts.

## Configuring a core caller

After `initTrace`, use `configureClock(TraceXTimerDirection.DECREASING, null)`
for a known decreasing target with unknown frequency, or pass `"1000000"` only
for a known 1 MHz counter. The pair is validated and persisted together before
installation. The existing direction/frequency setters delegate to the same
operation. Inspect `getTimerDirection()`, `getTicksPerSecond()`, and
`getProperties()` when recording evidence. `clearTicksPerSecond()` restores
ordering ticks. Callers must handle `TmfTraceException` and rejected frequency
values. These are Incubator internal APIs, not a stable public API contract.

Workspace trace resources persist settings; resource-free inputs do not. Clock
changes rebuild the logical index and invalidate analysis, supplementary data,
provider instances, and views. The desktop Clock dialog labels the timestamp
domain and rebuilds cached time-graph entries after a change. State-system tree
endpoints are inclusive; the generic time-graph view extends their drawing bound
by one timestamp unit.

`TraceXClockConfigurationSource` registers with the existing TMF configuration
extension. Its type ID is `org.eclipse.tracecompass.incubator.tracex.core.clock`.
The server's existing `/config/types/{typeId}/configs` routes expose it without
UI or REST dependencies in this bundle. Parameters are `trace_uri` (the imported
file location), `timer_direction` (`increasing` or `decreasing`), and optional
`ticks_per_second` (a decimal string). Omitting frequency clears physical time.
A stable resource-based configuration ID survives source disposal and restart;
only customized clocks are listed, and deleting one restores default settings.

Close all experiments containing the trace before applying server clock changes,
then reopen them. The source rejects resources owned by open traces or experiment
members because a merged experiment cannot safely retain its previous time
domain. It also rejects ambiguous imported locations, invalid fields, unsupported
keys, and updates with a mismatched configuration ID. See the user guide for
complete REST requests and the configuration-source tests for these contracts.

## Extending metadata and analysis

Pin the component header revision and its information-word contract before adding
an event. Add a catalog entry with its normalized ID, symbolic name, category,
and value/address aliases. Preserve raw information words and unknown-event
fallback behavior. Add a generated record to `TraceXEventMetadataTest` that
checks alias types, unresolved addresses, registry-name resolution, high raw-ID
flags, and the expected symbolic name. Catalog coverage is intentionally limited
to the events exercised by the demos; importing a generic event does not imply
semantic coverage for its component.

A scheduling transition additionally requires a documented next-context contract
and a state-system test, including equal timestamps and ISR nesting. Do not infer
a transition from an ordinary service name. The analysis is single-core and uses
stack order to match ISR exits, not an ISR-number correlation key. The factory
composes per-trace providers for experiments, retaining the same desktop/server
semantics. Broader pointer-width or SMP support requires a proven input contract
rather than reinterpretation of the existing 32-bit fields.

The TraceX integration effort originated in
[Eclipse Trace Compass Incubator pull request #212][origin]. That earlier
implementation was consulted only to corroborate the 48-byte header and
32-byte event record sizes; no production parser code was reused. The binary
contract and validation rules here come from the published TraceX format,
with current ThreadX headers and initialization behavior used as corroboration.

[format]: https://github.com/eclipse-threadx/rtos-docs-asciidoc/blob/main/rtos-docs/tracex/modules/ROOT/pages/chapter11.adoc
[origin]: https://github.com/eclipse-tracecompass-incubator/org.eclipse.tracecompass.incubator/pull/212
