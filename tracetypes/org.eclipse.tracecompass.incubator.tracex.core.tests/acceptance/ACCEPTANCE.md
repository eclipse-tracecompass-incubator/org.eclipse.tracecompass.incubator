# TraceX acceptance status

Acceptance remains incomplete for the gaps below. The evidence records a clean
supplied-release installation, interactive Linux workflows, independent demo
comparisons, shared desktop/server clock controls, and all platform builds.

## Verified evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Root verification and products | Complete 147-module verification passed; all six Linux, Windows, and macOS desktop/server archives built | `verification.json`, `products.json` |
| Core, SWTBot, server tests | 66 core, 3 SWTBot, and 79 server tests passed; zero failures/errors/skips | `verification.json` |
| Scoped coverage | 25 classes; zero missed lines and branches; adapter exclusions are documented in `TESTING.md` | `verification.json` |
| Four binary demos | All 3,824 original records, registry objects, raw/masked/relative timestamps, histograms, and selected aliases matched independent legacy output | `golden/`, `summaries/`, `differential.json` |
| Equivalent endian fixtures | Complete logical models matched; both render initialization, worker, nested ISR, restored worker, and idle | `generate_fixtures.py`, `desktop/`, `server/` |
| Clean Linux release | Supplied Trace Compass 12.1 archive hashed; rebuilt TraceX feature installed successfully; application launched and restarted with the same workspace | `desktop.json` |
| Interactive desktop | Four demos and both fixtures imported; event contents inspected and fixture fields exported; endpoints, searching, filtering/clearing, selection, and execution rendering checked | `desktop.json`, `desktop/` |
| Desktop demo metadata | Statistics showed 950/950/950/974 records; complete Events table exports matched every decoded field and all event-type counts in the verified models | `desktop-comparison.json`, `desktop/*-statistics.png` |
| Desktop clock | Zero frequency rejected; two ticks per second gives a three-second fixture endpoint; clearing returns six ordering ticks; known frequency and ThreadX decreasing direction survived application restart | `desktop.json`, `desktop/` |
| Live server | Health UP; six inputs opened/indexed; shared provider discovered; tree/state requests COMPLETED; configured ThreadX ends at 156206 ticks | `server/` |
| Server clock | Known/unknown endpoints and all seven fixture states verified; active and invalid edits rejected; reset verified; decreasing ThreadX configuration survived server restart | `server/clock-workflow.json`, `server/restart.json` |
| Execution boundary tooltip | Before/after-bound requests return empty completed tooltips; final valid timestamp retains its context; rebuilt server requests and supplied-release hover at tick 7 passed | `TraceXExecutionAnalysisTest`, `server/tooltip-bounds.json`, `desktop/final-fixture.png` |
| Updated help delivered | Installed bundle contains generated installation/import/clock/server guidance | `desktop.json` |

The full root verification is reused when repository changes only update
acceptance utilities and recorded evidence. Build provenance and report hashes
are recorded in `verification.json`; product hashes and bundle inventories are
recorded in `products.json`. Screenshots are original captures of the supplied
release with the installed feature. They show Trace Compass's standard timestamp
format; an ordering-ticks domain is explicitly identified by the execution view.
Demo frequency remains unknown and those displayed values are not physical time.

## Differential interpretation

| Demo | Input events | Legacy displayed events | Objects | Event types | Direction | Last relative tick |
| --- | ---: | ---: | ---: | ---: | --- | ---: |
| FileX | 950 | 950 | 6 | 25 | increasing | 949000 |
| NetX Duo TCP | 950 | 950 | 18 | 36 | increasing | 949000 |
| NetX Duo UDP | 950 | 951 | 16 | 23 | increasing | 949000 |
| ThreadX | 974 | 974 | 15 | 11 | decreasing | 156206 |

The legacy [TML implementation][legacy] inserts one synthetic UDP `Running` event
at legacy rank 538, context 63472, timestamp 50902012, relative tick 537012, with
four zero information words and no binary input record. All original records
match. The maintainer accepted input-record-only behavior on 2026-10-12 and asked
to retain the documented difference. The insertion remains in the golden capture
and the comparator still exits nonzero; approval does not erase the difference.

The ThreadX mask is 65535. Core preserves unsigned raw timer words while legacy
also exposes masked values; the capture adapter observes words before masking.
Legacy infers decreasing direction. Core requires that explicit setting, producing
the same 156206 elapsed ticks. Its unconfigured increasing endpoint is 63610322.
The current server evidence applies decreasing direction before opening ThreadX.
No physical-duration claim is made for any demo.

## Acceptance gaps

| Required evidence or behavior | Status and closure |
| --- | --- |
| Arbitrary event-table column sorting | The supplied release keeps chronological event-rank order and exposes column management, search, and filter controls. Arbitrary column sorting is unavailable. A decision to accept chronological ordering as this criterion's interpretation remains pending. |
| Interactive installed help | Help Contents could not launch because the environment has no configured web browser. Updated generated HTML is installed and inspected; opening it through the Help UI on a browser-equipped host remains pending. |
| Windows native smoke | Desktop/server products built; native launch/import/execution-view checks pending; owner unassigned. |
| macOS native smoke | Desktop/server products built; native launch/import/execution-view checks pending; owner unassigned. |
| Human review | Request two committer reviews when the contribution is authorized. No review approval is claimed by this evidence. |

Binary 32-bit input and single-core execution semantics are supported. Wider
pointer formats, SMP, comprehensive component catalogs, configurable user-event
metadata, and additional performance analyses remain future work.

[legacy]: https://github.com/eclipse-threadx/tracex/blob/b1af4decb38d48958ce3fa5c70d403f0d48aa25b/src/ELTMLManaged/tml_library.cpp
