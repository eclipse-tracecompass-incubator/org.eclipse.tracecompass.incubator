#!/usr/bin/env python3
# Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
# All rights reserved. This program and the accompanying materials are
# made available under the terms of the Eclipse Public License 2.0.
# SPDX-License-Identifier: EPL-2.0

"""Create a portable acceptance summary from a complete decoder export."""

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path

REVISION = 'b1af4decb38d48958ce3fa5c70d403f0d48aa25b'
RECORD_FIELDS = ('context', 'raw_priority', 'raw_event_id', 'event_id',
                 'raw_timestamp', 'masked_timestamp', 'relative_ticks', 'info')


def summarize(trace, model):
    """Retain timestamps, endpoints, registry, histogram, aliases, and digest."""
    events = model['events']
    records = [{k: e[k] for k in RECORD_FIELDS} for e in events]
    selected = []
    seen = set()
    for event in events:
        if event['event_id'] in seen:
            continue
        seen.add(event['event_id'])
        selected.append({'rank': event['rank'], 'event_id': event['event_id'],
                         'name': event['name'], 'fields': event['fields']})
    return {
        'schema_version': 1,
        'sample': trace.name,
        'input_sha256': hashlib.sha256(trace.read_bytes()).hexdigest(),
        'source_url': 'https://github.com/eclipse-threadx/tracex/blob/' + REVISION + '/src/MsixPackageProject/TraceFiles/' + trace.name,
        'byte_order': model['byte_order'],
        'timer_mask': model['timer_mask'],
        'timer_direction': model['timer_direction'],
        'event_count': len(events),
        'registry': model['registry'],
        'first_event': records[0] if records else None,
        'last_event': records[-1] if records else None,
        'histogram': dict(sorted(Counter(str(e['event_id']) for e in events).items(), key=lambda item: int(item[0]))),
        'raw_timestamps': [e['raw_timestamp'] for e in events],
        'masked_timestamps': [e['masked_timestamp'] for e in events],
        'relative_ticks': [e['relative_ticks'] for e in events],
        'selected_fields': selected,
        'logical_records_sha256': hashlib.sha256(json.dumps(records, sort_keys=True, separators=(',', ':')).encode()).hexdigest(),
        'diagnostics': model['diagnostics'],
        'legacy_only_events': model.get('legacy_only_events', []),
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('trace', type=Path)
    parser.add_argument('model', type=Path)
    args = parser.parse_args()
    print(json.dumps(summarize(args.trace, json.loads(args.model.read_text())), indent=2))
