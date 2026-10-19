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
| Typed aspects, trace-local TIDs, duplicate names, incomplete registries, and unchanged event fields | `TraceXAspectsTest`, `TraceXStockViewsTest` |
| Stock view discovery, experiment composition, deferred IRQ switches, CPU usage, and clock refresh | `TraceXStockViewsTest`, `TraceXKernelStateTest` |
| Core extension registration, headless dependencies, and bundle lifecycle | `ExtensionRegistrationTest`, `ActivatorTest` |
| Import, event selection/filtering, dialog validation, unknown-time clearing, clock persistence, refreshed execution view, and cross-selection, stock views, and pinned-view clock refresh | `TraceXUiWorkflowTest` |

## Coverage gate

The normal Maven `verify` phase fails unless the binary parser/model classes and
the shared execution state provider, kernel projection, stock-view adapters,
and event aspects have zero missed lines and zero missed branches.
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
mvn -Dtarget-platform=tracecompass-incubator-12.1 -pl :org.eclipse.tracecompass.incubator.tracex.core,:org.eclipse.tracecompass.incubator.tracex.core.tests clean verify
```

The complete verification against the Java 21 release baseline is:

```sh
mvn -Dtarget-platform=tracecompass-incubator-12.1 clean verify
```

For a focused build in a fresh checkout, install the target definitions first:

```sh
mvn -pl common/org.eclipse.tracecompass.incubator.target -am install -DskipTests
```

The default target is the Java 21 release baseline. The explicit rolling target
`tracecompass-incubator-master` currently requires Java 25; record the selected
target with build results.

For reproducible headless GTK rendering, use an isolated X11 display and fixed
scaling. This avoids inheriting a desktop Wayland session:

```sh
env -u WAYLAND_DISPLAY GDK_BACKEND=x11 GDK_SCALE=1 GDK_DPI_SCALE=1 \
  xvfb-run -a -s '-screen 0 1920x1080x24 -dpi 96' \
  mvn -Dtarget-platform=tracecompass-incubator-12.1 \
  '-Dbase.ui.test.vmargs=-Dorg.eclipse.swt.browser.DefaultType=mozilla -Dswt.autoScale=100' \
  clean verify
```

The stock-view workflow retains screenshots under the SWTBot bundle's
`screenshots` directory for inspection. A passing widget assertion alone does
not establish correct rendering on another display or native platform.
