# Native Windows acceptance result

The native Windows run on 2026-10-14 through 2026-10-15 reported **PASS with
retained diagnostics**. The tested head was
`b65e0a8d89fb440967d8db7df24f666bf33eb9de`, using product build `20261013-2127`,
TraceX feature `0.19.0.202610132127`, core/UI `0.1.0.202610132127`, and
documentation `0.19.0.202610132127`. Source, product, and stock-release identities
are recorded in [`windows.json`](../windows.json).

## Functional results

| Workflow | Result |
| --- | --- |
| Clean Trace Compass 12.1 installation, trust prompts, and restart | Passed |
| Stock and bundled native Windows desktop launchers, fresh workspaces | Passed |
| All four demos and both endian fixtures | Passed |
| Both fixture execution sequences, including nested ISR restoration and Idle | Passed |
| Search, two-of-seven filtering, clear, cross-selection, and ISR fields | Passed |
| Idle tooltip and exclusive tick-7 boundary | Passed; no TimeRangeException |
| Invalid frequency, known/unknown clocks, reset, and two process restarts | Passed |
| Installed and bundled TraceX Help | Passed |
| Native Windows server provider and clock workflows | Passed |
| Native server restart | Passed; decreasing/unknown ThreadX, 974 events, end 156206, completed tree and states |

| Demo | Events | Event types | Timer direction | Final relative tick |
| --- | ---: | ---: | --- | ---: |
| FileX | 950 | 25 | increasing | 949000 |
| NetX TCP | 950 | 36 | increasing | 949000 |
| NetX UDP | 950 | 23 | increasing | 949000 |
| ThreadX | 974 | 11 | decreasing | 156206 |

Demo frequencies remain unknown. Both endian fixtures contain seven events and
produce Initialization, worker, ISR 7, ISR 8, ISR 7, worker, Idle at ticks 0–6.

The Windows comparison utility validated all 3,824 desktop-exported records:
raw timestamps, relative ticks, ranks, complete event-ID histograms, and selected
aliases. Subsequent evidence review matched **every decoded event field** against
the retained verified core models and checked each export hash. All four archive
hashes were independently checked against the actual Windows downloads. Captured
six-input server and restart results were also checked. The completed application
run was not repeated. See [`desktop-comparison.json`](desktop-comparison.json)
and the URI-redacted responses under [`server/`](server/).

## Retained diagnostics and review limits

During clock invalidation, the stock workspace logged three failed deletions of
open `statistics-totals.ht` and `statistics-types.ht` supplementary files through
`TraceXTrace.invalidateTimeDependentData`. Windows reported that the files were
in use. Required clock, event-table, execution-bound, endpoint, persistence, and
later full-export checks passed. No successful Statistics-file cleanup is claimed.
Keep this issue visible in review and investigate cleanup ownership/lifetime
separately; it was non-blocking for the recorded acceptance workflows.

The bundled desktop also logged unrelated CDT/uftrace `c++filt`/native-spawner
warnings and two preference-category warnings. Server detector and SLF4J
diagnostics were retained. No TraceX parsing, provider, endpoint, or restart
failure was reported.

Original screenshots, detailed handover, workspace/server logs, and exports stay
private. Their selected screenshot/export hashes are recorded for traceability.
The responses here remove deployment-specific trace locations while retaining
counts, timestamps, identifiers, state models, and validation outcomes.

The prior Linux/full-build results remain applicable because this integration
changes acceptance evidence and documentation only. Native macOS and the required
human reviews remain open; this Windows result does not establish either.
