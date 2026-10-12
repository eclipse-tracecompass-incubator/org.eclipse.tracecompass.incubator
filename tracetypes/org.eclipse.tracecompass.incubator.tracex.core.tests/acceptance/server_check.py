#!/usr/bin/env python3
# Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
# All rights reserved. This program and the accompanying materials are
# made available under the terms of the Eclipse Public License 2.0.
# SPDX-License-Identifier: EPL-2.0

"""Exercise a built Trace Server with public demos or generated execution fixtures."""

import argparse
import json
import time
import urllib.request
from pathlib import Path

PROVIDER = 'org.eclipse.tracecompass.incubator.tracex.core.dataprovider'
COUNTS = {'demo_filex': 950, 'demo_netx_tcp': 950, 'demo_netx_udp': 950, 'demo_threadx': 974}
FIXTURE_STATES = ['Initialization', 'worker [0x20001000]', 'ISR 7', 'ISR 8',
                  'ISR 7', 'worker [0x20001000]', 'Idle']


def portable(value):
    """Remove deployment-specific trace URIs from evidence, preserving their basenames."""
    if isinstance(value, dict):
        return {key: 'TRACE_URI/' + Path(item).name if key in ('path', 'uri') and isinstance(item, str)
                else portable(item) for key, item in value.items()}
    if isinstance(value, list):
        return [portable(item) for item in value]
    return value


def run(base_url, traces, output):
    """Check discovery and complete tree/state results and save portable HTTP evidence."""
    output.mkdir(parents=True, exist_ok=True)

    def request(method, endpoint, data=None):
        """Make one JSON request and retain its status, parameters, and response."""
        body = json.dumps(data).encode() if data is not None else None
        query = urllib.request.Request(base_url + endpoint, body,
                                       {'Content-Type': 'application/json'}, method=method)
        with urllib.request.urlopen(query, timeout=30) as response:
            result = {'method': method, 'endpoint': endpoint, 'request': data,
                      'http_status': response.status, 'response': json.load(response)}
        return portable(result)

    def save(name, evidence):
        """Write one portable request and response artifact."""
        (output / name).write_text(json.dumps(evidence, indent=2) + '\n')

    health = request('GET', '/health')
    assert health['response']['status'] == 'UP', health
    save('health.json', health)
    inputs = sorted(p for p in traces.glob('*.trx') if p.stem in COUNTS or p.name in ('little-execution.trx', 'big-execution.trx'))
    if not inputs:
        raise ValueError('No public demos or generated execution fixtures found')
    for trace in inputs:
        name = trace.stem
        opened = request('POST', '/traces', {'parameters': {'name': trace.name, 'uri': str(trace.resolve()),
                         'typeID': 'org.eclipse.tracecompass.incubator.tracex.core.trace'}})
        save(name + '.trace.json', opened)
        created = request('POST', '/experiments', {'parameters': {'name': name,
                           'traces': [opened['response']['UUID']]}})
        save(name + '.create-experiment.json', created)
        uid = created['response']['UUID']
        for _ in range(100):
            experiment = request('GET', '/experiments/' + uid)
            if experiment['response']['indexingStatus'] == 'COMPLETED':
                break
            time.sleep(0.1)
        assert experiment['response']['indexingStatus'] == 'COMPLETED', experiment
        assert experiment['response']['nbEvents'] == COUNTS.get(name, 7), experiment
        save(name + '.experiment.json', experiment)
        outputs = request('GET', '/experiments/' + uid + '/outputs')
        assert any(item['id'] == PROVIDER for item in outputs['response']), outputs
        save(name + '.outputs.json', outputs)
        endpoint = '/experiments/' + uid + '/outputs/timeGraph/' + PROVIDER
        start, end = experiment['response']['start'], experiment['response']['end']
        for _ in range(100):
            tree = request('POST', endpoint + '/tree', {'parameters': {'requested_times': [start, end]}})
            if tree['response']['status'] == 'COMPLETED':
                break
            time.sleep(0.1)
        assert tree['response']['status'] == 'COMPLETED', tree
        save(name + '.tree.json', tree)
        entries = tree['response']['model']['entries']
        cpu = next(entry for entry in entries if entry['labels'] == ['Execution'])
        times = list(range(7)) if name not in COUNTS else [start + (end - start) * i // 100 for i in range(101)]
        states = request('POST', endpoint + '/states', {'parameters': {'requested_times': times,
                          'requested_items': [entry['id'] for entry in entries if entry.get('hasData')]}})
        assert states['response']['status'] == 'COMPLETED', states
        save(name + '.states.json', states)
        rows = states['response']['model']['rows']
        cpu_states = next(row['states'] for row in rows if row['entryId'] == cpu['id'])
        assert cpu_states, states
        if name not in COUNTS:
            assert [state['label'] for state in cpu_states] == FIXTURE_STATES, states
            assert [(state['start'], state['end']) for state in cpu_states] == [(i, i + 1) for i in range(7)], states
        print(name, experiment['response']['nbEvents'], 'events; tree/states COMPLETED')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://localhost:8080/tsp/api')
    parser.add_argument('traces', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    run(args.base_url.rstrip('/'), args.traces, args.output)
