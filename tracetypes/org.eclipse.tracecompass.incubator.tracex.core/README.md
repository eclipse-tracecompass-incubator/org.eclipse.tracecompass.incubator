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

To extend the catalog, add an entry to `TraceXMetadataCatalog` using the event ID,
symbolic name, component category, and aliases from the corresponding current
component header. An address alias enables registry-name resolution but never
replaces the underlying unsigned information word.

### Demo compatibility check

The decoder output matches independently captured legacy summaries for the four
bundled demonstrations:

| Demonstration | Registry objects (legacy/new) | Valid events (legacy/new) | Distinct event types (legacy/cataloged) | Unresolved contexts |
| --- | ---: | ---: | ---: | ---: |
| FileX | 6 / 6 | 950 / 950 | 25 / 25 | 0 |
| NetX Duo TCP | 18 / 18 | 950 / 950 | 36 / 36 | 0 |
| NetX Duo UDP | 16 / 16 | 950 / 950 | 23 / 23 | 0 |
| ThreadX | 15 / 15 | 974 / 974 | 11 / 11 | 0 |

The NetX Duo UDP trace also contains 27 ISR contexts and the ThreadX trace
contains 8. Those sentinels and their interrupted threads resolve separately
from ordinary object addresses and are not counted as unresolved contexts.

The TraceX integration effort originated in
[Eclipse Trace Compass Incubator pull request #212][origin]. That earlier
implementation was consulted only to corroborate the 48-byte header and
32-byte event record sizes; no production parser code was reused. The binary
contract and validation rules here come from the published TraceX format,
with current ThreadX headers and initialization behavior used as corroboration.

[format]: https://github.com/eclipse-threadx/rtos-docs-asciidoc/blob/main/rtos-docs/tracex/modules/ROOT/pages/chapter11.adoc
[origin]: https://github.com/eclipse-tracecompass-incubator/org.eclipse.tracecompass.incubator/pull/212
