#!/usr/bin/env python3
# Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
# All rights reserved. This program and the accompanying materials are
# made available under the terms of the Eclipse Public License 2.0.
# SPDX-License-Identifier: EPL-2.0

"""Summarize original legacy records while retaining every synthetic insertion."""

import argparse
import json
from pathlib import Path

from summarize import summarize

# Information-word contracts from the component headers linked in README.md.
CONTRACTS = {
    'demo_filex': (206, 'FX_TRACE_INTERNAL_IO_DRIVER_READ',
                   ('media', 'sector', 'sector_count', 'buffer'), (1, 4)),
    'demo_netx_tcp': (386, 'NX_TRACE_PACKET_ALLOCATE',
                      ('packet_pool', 'packet', 'packet_type', 'available_packets'), (1, 2)),
    'demo_netx_udp': (386, 'NX_TRACE_PACKET_ALLOCATE',
                      ('packet_pool', 'packet', 'packet_type', 'available_packets'), (1, 2)),
    'demo_threadx': (1, 'TX_TRACE_THREAD_RESUME',
                     ('thread', 'previous_state', 'stack', 'next_thread'), (1, 3, 4)),
}


def legacy_summary(trace, legacy, direction):
    """Build a summary directly from TML output and independently specified aliases."""
    if direction != legacy['timer_direction']:
        raise ValueError('Requested direction differs from legacy reconstruction')
    raw = trace.read_bytes()
    order = 'little' if raw[:4] == b'BTXT' else 'big'
    model = {'byte_order': order.upper() + '_ENDIAN',
             'timer_mask': int.from_bytes(raw[4:8], order),
             'timer_direction': direction,
             'registry': legacy['registry'], 'diagnostics': [], 'events': [],
             'legacy_only_events': [{k: e[k] for k in ('rank', 'context', 'event_id', 'masked_timestamp', 'relative_ticks', 'info')}
                                    for e in legacy['events'] if not e['source_record']]}
    event_id, name, aliases, addresses = CONTRACTS[trace.stem]
    registry = {r['address']: r['name'] for r in legacy['registry']}
    for rank, event in enumerate(e for e in legacy['events'] if e['source_record']):
        record = dict(event, rank=rank, name='Event ' + str(event['event_id']), fields={})
        if event['event_id'] == event_id:
            record['name'] = name
            for word, alias in enumerate(aliases, 1):
                value = event['info'][word - 1]
                record['fields'][alias] = value
                if word in addresses:
                    record['fields'][alias + '_address'] = f'0x{value:08X}'
                    if value in registry:
                        record['fields'][alias + '_name'] = registry[value]
        model['events'].append(record)
    result = summarize(trace, model)
    result['selected_fields'] = [s for s in result['selected_fields'] if s['event_id'] == event_id]
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('trace', type=Path)
    parser.add_argument('model', type=Path)
    parser.add_argument('direction', choices=('increasing', 'decreasing'))
    args = parser.parse_args()
    print(json.dumps(legacy_summary(args.trace, json.loads(args.model.read_text()), args.direction), indent=2))
