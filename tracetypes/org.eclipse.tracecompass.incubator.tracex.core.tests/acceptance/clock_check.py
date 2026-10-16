#!/usr/bin/env python3
# Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
# All rights reserved. This program and the accompanying materials are
# made available under the terms of the Eclipse Public License 2.0.
# SPDX-License-Identifier: EPL-2.0

"""Verify the shared server clock workflow with a generated execution trace."""

import argparse
import json
import time
import urllib.error
import urllib.request
from pathlib import Path
from server_check import portable, PROVIDER

TYPE = 'org.eclipse.tracecompass.incubator.tracex.core.clock'


def run(base, trace, output):
    """Assert known/unknown time, rejection, persistence, and provider refresh."""
    output.mkdir(parents=True, exist_ok=True)
    evidence = []

    def request(method, endpoint, data=None, status=200):
        """Retain each response, including expected validation failures."""
        query = urllib.request.Request(base + endpoint,
                json.dumps(data).encode() if data is not None else None,
                {'Content-Type': 'application/json'}, method=method)
        try:
            response = urllib.request.urlopen(query, timeout=30)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            body = json.loads(response.read())
            evidence.append(portable({'method': method, 'endpoint': endpoint,
                'request': data, 'http_status': response.status, 'response': body}))
            assert response.status == status, evidence[-1]
            return body

    def open_experiment(name, uid, expected):
        """Verify the indexed endpoint and shared provider's rebuilt tree."""
        result = request('POST', '/experiments', {'parameters': {'name': name, 'traces': [uid]}})
        experiment_id = result['UUID']
        for _ in range(100):
            result = request('GET', '/experiments/' + experiment_id)
            if result['indexingStatus'] == 'COMPLETED':
                break
            time.sleep(0.1)
        assert result['indexingStatus'] == 'COMPLETED', result
        assert result['nbEvents'] == 7 and result['end'] == expected, result
        outputs = request('GET', '/experiments/' + experiment_id + '/outputs')
        assert any(item['id'] == PROVIDER for item in outputs), outputs
        endpoint = '/experiments/' + experiment_id + '/outputs/timeGraph/' + PROVIDER
        for _ in range(100):
            tree = request('POST', endpoint + '/tree', {'parameters': {'requested_times': [0, expected]}})
            if tree['status'] == 'COMPLETED':
                break
            time.sleep(0.1)
        assert tree['status'] == 'COMPLETED', tree
        cpu = next(entry for entry in tree['model']['entries'] if entry['labels'] == ['Execution'])
        states = request('POST', endpoint + '/states', {'parameters': {
            'requested_times': [expected * i // 6 for i in range(7)], 'requested_items': [cpu['id']]}})
        assert states['status'] == 'COMPLETED', states
        labels = [state['label'] for state in states['model']['rows'][0]['states']]
        assert labels == ['Initialization', 'worker [0x20001000]', 'ISR 7', 'ISR 8',
                          'ISR 7', 'worker [0x20001000]', 'Idle'], states
        return experiment_id

    imported = request('POST', '/traces', {'parameters': {'name': trace.name,
        'uri': str(trace.resolve()), 'typeID': 'org.eclipse.tracecompass.incubator.tracex.core.trace'}})
    route = '/config/types/' + TYPE + '/configs'
    parameters = {'trace_uri': str(trace.resolve()), 'timer_direction': 'increasing', 'ticks_per_second': '2'}
    config = request('POST', route, {'name': 'Execution clock', 'parameters': parameters})
    config_route = route + '/' + config['id']
    assert request('GET', config_route)['parameters']['ticks_per_second'] == '2'
    experiment = open_experiment('known-clock', imported['UUID'], 3_000_000_000)
    request('PUT', config_route, {'name': 'Rejected active clock', 'parameters': parameters}, status=400)
    request('DELETE', '/experiments/' + experiment)
    invalid = dict(parameters, ticks_per_second='0')
    request('PUT', config_route, {'name': 'Rejected frequency', 'parameters': invalid}, status=400)
    assert request('GET', config_route)['parameters']['ticks_per_second'] == '2'
    parameters.pop('ticks_per_second')
    request('PUT', config_route, {'name': 'Unknown time', 'parameters': parameters})
    experiment = open_experiment('unknown-clock', imported['UUID'], 6)
    request('DELETE', '/experiments/' + experiment)
    request('POST', route, {'name': 'Decreasing clock', 'parameters': dict(parameters, timer_direction='decreasing')})
    request('DELETE', config_route)
    assert not any(item['id'] == config['id'] for item in request('GET', route))
    experiment = open_experiment('reset-clock', imported['UUID'], 6)
    request('DELETE', '/experiments/' + experiment)
    (output / 'clock-workflow.json').write_text(json.dumps(evidence, indent=2) + '\n')
    print('Clock workflow passed: known/unknown bounds, state labels, active/invalid rejection, and reset')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://localhost:8080/tsp/api')
    parser.add_argument('trace', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    run(args.base_url.rstrip('/'), args.trace, args.output)
