#!/usr/bin/env python3
# Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
# All rights reserved. This program and the accompanying materials are
# made available under the terms of the Eclipse Public License 2.0.
# SPDX-License-Identifier: EPL-2.0

"""Compare independently captured models without hiding legacy-only events."""

import argparse
import json
from pathlib import Path

FIELDS = ('context', 'raw_priority', 'raw_event_id', 'event_id',
          'raw_timestamp', 'masked_timestamp', 'relative_ticks', 'info')


def compare(legacy, current):
    """Report every event difference and each independently matched raw record."""
    differences = []
    for key in ('byte_order', 'timer_mask', 'timer_direction'):
        if legacy[key] != current[key]:
            differences.append({'field': key, 'legacy': legacy[key], 'current': current[key]})
    if legacy['registry'] != current['registry']:
        differences.append({'field': 'registry', 'legacy': legacy['registry'], 'current': current['registry']})
    before, after = legacy['events'], current['events']
    i = j = matched = 0
    while i < len(before) and j < len(after):
        a, b = before[i], after[j]
        if a.get('source_record', 1) and all(a[k] == b[k] for k in FIELDS):
            i += 1
            j += 1
            matched += 1
            continue
        # Preserve the insertion as a difference, including its exact location.
        if (a['event_id'] == 6 and a['info'] == [0, 0, 0, 0]
                and i + 1 < len(before)
                and all(before[i + 1][k] == b[k] for k in FIELDS)):
            differences.append({'field': 'legacy_only_event', 'legacy_rank': i,
                                'before_current_rank': j,
                                'event': {k: a[k] for k in FIELDS if k not in ('raw_priority', 'raw_event_id', 'raw_timestamp')}})
            i += 1
            continue
        for key in FIELDS:
            if a[key] != b[key]:
                differences.append({'legacy_rank': i, 'current_rank': j,
                                    'field': key, 'legacy': a[key], 'current': b[key]})
        i += 1
        j += 1
    if i != len(before) or j != len(after):
        differences.append({'field': 'unmatched_tail', 'legacy_remaining': len(before) - i, 'current_remaining': len(after) - j})
    return {'legacy_event_count': len(before), 'current_event_count': len(after),
            'matched_original_records': matched, 'differences': differences}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('legacy', type=Path)
    parser.add_argument('current', type=Path)
    args = parser.parse_args()
    report = compare(json.loads(args.legacy.read_text()), json.loads(args.current.read_text()))
    print(json.dumps(report, indent=2))
    raise SystemExit(1 if report['differences'] else 0)
