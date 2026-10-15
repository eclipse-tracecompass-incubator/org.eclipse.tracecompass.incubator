# TraceX test coverage

The core test bundle generates every fixture at runtime from the binary layout
documented in the ThreadX TraceX User Guide. Tests do not require captured trace
files or another working tree.

## Reviewer matrix

| Behavior | Test class |
| --- | --- |
| Header signatures, byte order, unsigned ranges, truncation, alignment, and checked arithmetic | `TraceXParserTest` |
| Registry sizes, names, object types, unused records, duplicate addresses, and malformed records | `TraceXRegistryTest` |
| Event families, aliases, field types, sentinels, priorities, thresholds, and unknown events | `TraceXEventMetadataTest` |
| Partial and circular buffers, pointer boundaries, logical ordering, seeks, and cancellation | `TraceXEventIndexTest` |
| Trace confidence, properties, repeated/concurrent reads, cancellation, ownership, and disposal | `TraceXTraceTest` |
| Timer masks, directions, wraps, equal ticks, conversion limits, persistence, and invalidation | `TraceXTimeNormalizerTest`, `TraceXClockConfigurationTest` |
| Execution identities, every transition, nested/incomplete interrupts, equal times, and diagnostics | `TraceXExecutionContextTest`, `TraceXExecutionAnalysisTest` |
| Provider identifiers, trees, rows, filtering, styles, tooltips, selection, and cancellation | `TraceXExecutionAnalysisTest`, `TraceXClockConfigurationTest` |
| Configuration-source discovery, atomic clock updates, imported-resource CRUD, reopen/reset, and active-trace rejection | `TraceXClockConfigurationSourceTest`, `TraceXClockConfigurationTest` |
| Core extension registration, headless dependencies, and bundle lifecycle | `ExtensionRegistrationTest`, `ActivatorTest` |
| Import, event selection/filtering, dialog validation, unknown-time clearing, clock persistence, refreshed execution view, and cross-selection | `TraceXUiWorkflowTest` |

## Coverage gate

The normal Maven `verify` phase fails unless the binary parser/model classes and
the execution state provider have zero missed lines and zero missed branches.
The aggregate report reads the same execution data produced by the Tycho test
runtime.

The gate excludes `TraceXTrace`, `TraceXDataProvider`,
`TraceXDataProviderFactory`, `TraceXClockConfigurationSource`, bundle activators,
and the SWT view/dialog. These classes
adapt the covered logic to Trace Compass lifecycle, data-provider, extension,
and widget APIs. Their observable contracts are exercised by the core
integration tests, server-product builds, and SWTBot workflow test instead of by
branch-forcing unit tests.

Run the focused gate with:

```sh
mvn -pl :org.eclipse.tracecompass.incubator.tracex.core,:org.eclipse.tracecompass.incubator.tracex.core.tests clean verify
```

The complete CI-equivalent verification remains:

```sh
mvn clean verify
```
