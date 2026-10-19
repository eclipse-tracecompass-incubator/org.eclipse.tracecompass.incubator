# Standard OS view verification

The integration adapts Matthew Khouzam's event aspects, kernel adapter, and trace
icons from [PR #284][proposal], commit
`72ad7baa6781e22d10dde99623fb9a14dd685604`. The existing TraceX execution engine
supplies Control Flow, Resources, and CPU Usage. Original event names and the
complete Contents fields remain available.

| Check | Result |
| --- | --- |
| Core | 77 tests passed, including real provider discovery, experiments, duplicate names, priorities, deferred and nested interrupts, unsigned IRQ boundaries, CPU totals, original fields, and clock invalidation |
| Coverage | 36 scoped classes; zero missed lines and branches; existing adapter exclusions remain in `TESTING.md` |
| Desktop | Four SWTBot tests passed; stock views populated, Events retained Context and Contents, and growing/shrinking clock changes preserved pinned Control Flow |
| Rendering | Stock-view screenshots inspected on isolated X11 at 1920×1080, 96 DPI, and 100% SWT scaling |
| Server | 79 existing server tests passed; final packaged-server requests returned completed trees and populated states or XY series for all three stock outputs |
| Unsigned IRQs | Final server accepted nested IRQs 4294967295 and 2147483648; Resources retained both values in its labels |
| Products | All 147 reactor modules assembled; six Linux, Windows, and macOS desktop/server archives built; packaged TraceX classes matched verified compiled classes and included all icon resolutions |
| Installation | Final feature and its analysis-core dependency installed into a fresh copy of the supplied Trace Compass 12.1 release |

`stock-views-verification.json` records report and archive hashes, source revision,
target, and validation provenance. The final 77-test core suite covers the IRQ
boundary correction. The desktop and server suites exercised unchanged
normal-range behavior before that correction; their passed evidence is reused.
Final product assembly used `verify -DskipTests`. Live REST checks were repeated
on the final packaged server for both ordinary and unsigned-IRQ inputs, each as a
single-trace experiment and a two-trace experiment. Portable responses are in
`stock-views/server/` and its `unsigned-irqs/` subdirectory. The CPU series at ticks
0 through 6 is `[0, 0, 100, 0, 0, 0, 100]`, excluding the initialization and ISR
intervals from thread execution.

For reproduction, use the release-target and isolated-display commands in
`../TESTING.md`. Generate the seven-event fixtures with `generate_fixtures.py`.
For the unsigned case, replace the outer ISR number 7 with 4294967295 and the
inner number 8 with 2147483648 in both entry and exit records. Discover the stock
output IDs through `GET /experiments/EXPERIMENT_UUID/outputs`, then request their
`timeGraph/OUTPUT_ID/tree` and `states`, or `XY/OUTPUT_ID/tree` and `xy`, endpoints
with ticks 0 through 6 and the data entry IDs returned by each tree.

Headless GTK tests must select X11 explicitly. Inheriting a Wayland session can
bypass Xvfb and make screenshots unreliable. The retained rendering check uses
isolated X11; native platform rendering remains separate.

The default build was subsequently verified with no target override: all 147
modules passed `clean install`, including the final 77 core, four UI, and 79
server tests and six product builds. Coverage remained complete across 36 scoped
classes. CI now uses the same release target and fixed X11 scaling. The additive
`default_build` record contains report and screenshot hashes; earlier archive
hashes and live-server evidence still identify the separately verified products.

Native Windows checks for this follow-up remain pending; the previous Windows
record predates these changes. Follow the added stock-view procedure in
`WINDOWS.md`. Native macOS remains untested. The explicit rolling master
target requires Java 25 and remains unverified; the default now selects the
verified Java 21 release target. Critical-path
and wakeup/blocking-cause analyses remain unsupported.

[proposal]: https://github.com/eclipse-tracecompass-incubator/org.eclipse.tracecompass.incubator/pull/284
