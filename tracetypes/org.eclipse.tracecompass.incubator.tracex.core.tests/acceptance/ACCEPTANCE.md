# TraceX acceptance status

Acceptance is incomplete. The retained build/test results and new differential
and REST checks establish the behavior below. They do not establish installation
into an unmodified supplied release or a complete interactive desktop pass.

## Verified evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Root verification and products | Reused successful 147-module Maven verify; Linux, Windows, and macOS desktop/server archives contain the intended TraceX bundles | `products.json`, verification commit `b4b50aa02db6ae98187ff00a0782e88fa2be76ae` |
| Core tests | 58 passed, zero failures/errors/skips | `verification.json` |
| SWTBot | Both tests passed, including generated import, filters, cross-selection, and API-configured clock persistence | `verification.json` |
| Server regression tests | 79 passed, zero failures/errors/skips | `verification.json` |
| Scoped coverage | 25 classes; zero missed lines and branches, with the exclusions documented in `TESTING.md` | `verification.json` |
| Four binary demos | 3,824 original records decoded; no registry diagnostics; all registry and record fields matched independent legacy TML output | `golden/`, `summaries/`, `differential.json` |
| Selected aliases | Independently specified information-word contracts and registry names matched on all four demos | `golden/`, component links in `README.md` |
| Equivalent endian fixtures | Complete seven-event logical models, decoded fields, and registry matched | `generate_fixtures.py`, `server/` |
| Live server products | Health UP; four demos and both fixtures opened; experiments indexed; shared provider advertised; trees and states COMPLETED | `server/` |
| Execution semantics | Fixture states include initialization, thread execution, nested ISR entry/exit, restoration, and idle; direct provider tests cover scheduler changes within ISR, equal timestamps, boundary contexts, and diagnostics | `server/`, retained `TraceXExecutionAnalysisTest` results |

The product archives and runtime TraceX core use the same production sources as
the starting verification commit. New acceptance utilities run outside the Maven
source folders. User-help generation is checked separately after the prose edit;
retained product archives still contain the earlier help bundle. No new complete
product build is claimed for the updated documentation.

## Differential interpretation

| Demo | Input events | Legacy displayed events | Objects | Event types | Direction | Last relative tick |
| --- | ---: | ---: | ---: | ---: | --- | ---: |
| FileX | 950 | 950 | 6 | 25 | increasing | 949000 |
| NetX Duo TCP | 950 | 950 | 18 | 36 | increasing | 949000 |
| NetX Duo UDP | 950 | 951 | 16 | 23 | increasing | 949000 |
| ThreadX | 974 | 974 | 15 | 11 | decreasing | 156206 |

The legacy [TML implementation][legacy] synthesizes a `Running` event for
sequential display when a predicted next context differs from the following
record. For UDP it inserts that event at legacy rank 538, context 63472, timestamp
50902012, relative tick 537012. It has four zero information words and no input
raw ID/priority/timestamp. The original 950 records all match. TraceX core emits
input records only. The insertion is retained in the golden summary and the
comparison exits nonzero; maintainer acceptance of this difference is pending.

The ThreadX mask is 65535. Core preserves unsigned raw timer words while the
legacy library exposes masked values; the capture adapter observes raw words
before masking, so both representations are compared independently. Legacy
infers decreasing direction for this demo. Core defaults to increasing and
requires explicit configuration, which yields the same 156206 elapsed ticks.
Its unconfigured increasing endpoint is 63610322; the server responses retain
that value and do not claim legacy timing equivalence. Frequency is unavailable
for every demo, so no physical-duration claim is made.

## Acceptance gaps

| Required evidence or behavior | Status and closure |
| --- | --- |
| Clean supplied Linux 12.1 RCP install/restart | The exact clean release was not available in this run. Install the p2 repository into it and verify compatibility; a built Incubator product is separate evidence. |
| Full interactive Linux workflow | Retained SWTBot results cover generated import/filter/selection/view behavior. Clean-release sorting, searching, field inspection, all-demo interactive verification, restart, and screenshots remain pending. |
| Clock configuration workflow | Core setters and persistence are tested. A desktop clock editor and TraceX REST clock-setting endpoint are absent; the SWTBot clock check calls the API directly. Complete the user-facing workflow before claiming this gate. |
| ThreadX server timing equivalence | Default increasing-domain requests succeeded. Configure decreasing direction through a core caller and record the correct server endpoint and state responses. |
| Legacy differential decision | Review the synthetic UDP event and explicit-direction policy; record maintainer acceptance. No unexplained record difference remains. |
| Windows/macOS runtime smoke | Product builds are retained; native launches/import/view checks are pending and owners are unassigned. |
| Screenshots and reviewer attachments | Earlier screenshots were not retained in the available artifacts. Capture current images and attach them with the acceptance evidence before publication. |
| Updated-help delivery | Documentation generation is verified separately; rebuild the p2 repository/products when preparing an installable contribution with the new help. |
| Human review | Two committer reviews and maintainer acceptance remain required. |

Expanded pointer-width support, SMP semantics, comprehensive USBX catalogs,
configurable user-event metadata, and additional performance analyses are future
work. They do not replace the outstanding acceptance checks above.

[legacy]: https://github.com/eclipse-threadx/tracex/blob/b1af4decb38d48958ce3fa5c70d403f0d48aa25b/src/ELTMLManaged/tml_library.cpp
