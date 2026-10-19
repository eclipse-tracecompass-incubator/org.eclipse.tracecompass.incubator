#!/usr/bin/env python3
# Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
# All rights reserved. This program and the accompanying materials are
# made available under the terms of the Eclipse Public License 2.0.
# SPDX-License-Identifier: EPL-2.0

"""Generate independent binary fixtures from the published 32-bit TraceX layout."""

import argparse
import struct
from pathlib import Path


def generate(output):
    """Write equivalent endian fixtures including nested interrupt transitions."""
    output.mkdir(parents=True, exist_ok=True)
    thread = 0x20001000
    # Initialization, thread, nested ISR, thread restoration, idle.
    events = [
        (0xF0F0F0F0, 0, 6, 100, 0, 0, 0, 0),
        (thread, 0x80050005, 6, 101, 0, 0, 0, 0),
        (0xFFFFFFFF, thread, 3, 102, 0, 7, 1, 0),
        (0xFFFFFFFF, thread, 3, 103, 0, 8, 2, 0),
        (0xFFFFFFFF, thread, 4, 104, 0, 8, 2, 0),
        (0xFFFFFFFF, thread, 4, 105, 0, 7, 1, 0),
        (thread, 0x80050005, 2, 106, thread, 0, 0, 0),
    ]
    for order, prefix in [('little', '<'), ('big', '>')]:
        base, registry_start, event_start = 0xF0000000, 48, 80
        header = struct.pack(prefix + 'IIIIHHIIIIIII',
                             0x54585442, 0xFFFF, base, base + registry_start,
                             0, 16, base + event_start, base + event_start,
                             base + event_start + 8 * 32,
                             base + event_start + len(events) * 32,
                             0xAAAAAAAA, 0xBBBBBBBB, 0xCCCCCCCC)
        registry = struct.pack(prefix + 'BBBBIII16s', 0, 1, 0x80, 5,
                               thread, 0x20002000, 0x400, b'worker\0')
        data = header + registry + b''.join(struct.pack(prefix + '8I', *e) for e in events) + bytes(32)
        (output / (order + '-execution.trx')).write_bytes(data)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output', type=Path)
    generate(parser.parse_args().output)
