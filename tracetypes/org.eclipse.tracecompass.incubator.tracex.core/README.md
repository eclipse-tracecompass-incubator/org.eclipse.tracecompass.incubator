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

The TraceX integration effort originated in
[Eclipse Trace Compass Incubator pull request #212][origin]. That earlier
implementation was consulted only to corroborate the 48-byte header and
32-byte event record sizes; no production parser code was reused. The binary
contract and validation rules here come from the published TraceX format,
with current ThreadX headers and initialization behavior used as corroboration.

[format]: https://github.com/eclipse-threadx/rtos-docs-asciidoc/blob/main/rtos-docs/tracex/modules/ROOT/pages/chapter11.adoc
[origin]: https://github.com/eclipse-tracecompass-incubator/org.eclipse.tracecompass.incubator/pull/212
