#!/usr/bin/env python3
# *****************************************************************************
# Copyright (c) 2026 Ericsson
#
# All rights reserved. This program and the accompanying materials are
# made available under the terms of the Eclipse Public License 2.0 which
# accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
# *****************************************************************************
"""Generate a small, synthetic SQLite trace for the SQLite trace-type tests.

The produced database mimics the shape the SQLite trace reader expects
without shipping a real-world capture:

  * a schema table (name ending in ``trace``) named ``_meta_trace`` that maps
    each event table to a dotted trace-point ``name`` plus ``severity``,
    ``source`` and ``full_text`` metadata;
  * two event tables that share the columns ``time``, ``flag``, ``counter1``
    and ``counter2`` so the reader's column deduplication is exercised, and
    one table-specific numeric column ``counter3``.

Columns:
  * ``time``     TEXT  ISO ``yyyy-MM-dd HH:mm:ss.ffffff`` (UTC), the event time
  * ``flag``     TEXT  a non-numeric label (stays a plain, non-counter aspect)
  * ``counter1`` INTEGER numeric gauge -> exposed as a counter aspect
  * ``counter2`` INTEGER numeric gauge -> exposed as a counter aspect
  * ``counter3`` INTEGER numeric gauge, only on ``sensor_alpha``

The generator is fully deterministic: running it always yields the same rows,
timestamps and, therefore, the same file. The tests assert against the
constants documented at the bottom of this file.

Usage:
    python3 generate_synthetic_trace.py [output.sqlite]

Defaults to writing ``synthetic_trace.sqlite`` next to this script.
"""

import datetime
import os
import sqlite3
import sys

# Fixed UTC start time. 2024-07-23 16:30:04.623337 UTC.
# Epoch nanos = 1721752204623337000 (matches the historical first timestamp).
_START = datetime.datetime(2024, 7, 23, 16, 30, 4, 623337,
                           tzinfo=datetime.timezone.utc)

# Microsecond step between consecutive events within a table.
_STEP_US = 751

# The dotted trace-point names, severities and event tables.
_SCHEMA_ROWS = [
    # table_name,     name,           severity, source,               full_text
    ("sensor_alpha", "sensor.alpha", "TRACE3", "src/sensor_alpha.c", "alpha flag=%s c1=%d c2=%d c3=%d"),
    ("sensor_beta",  "sensor.beta",  "TRACE3", "src/sensor_beta.c",  "beta flag=%s c1=%d c2=%d"),
]

# Number of event rows per table (11 + 11 = 22 total events).
_ALPHA_ROWS = 11
_BETA_ROWS = 11


def _fmt(dt):
    """Format a datetime as the ISO string the reader parses."""
    return dt.strftime("%Y-%m-%d %H:%M:%S.%f")


def _timestamp(index):
    """Deterministic timestamp for the given global event index."""
    return _START + datetime.timedelta(microseconds=_STEP_US * index)


def generate(path):
    if os.path.exists(path):
        os.remove(path)
    conn = sqlite3.connect(path)
    try:
        cur = conn.cursor()

        # Schema table: its name ends in "trace" so the reader treats it as
        # the external schema and skips it as an event table.
        cur.execute(
            "CREATE TABLE _meta_trace ("
            "table_name TEXT, name TEXT, severity TEXT, source TEXT, full_text TEXT)")
        cur.executemany(
            "INSERT INTO _meta_trace "
            "(table_name, name, severity, source, full_text) VALUES (?, ?, ?, ?, ?)",
            _SCHEMA_ROWS)

        # sensor_alpha: shared columns + a table-specific numeric column.
        cur.execute(
            "CREATE TABLE sensor_alpha ("
            "time TEXT, flag TEXT, counter1 INTEGER, counter2 INTEGER, counter3 INTEGER)")
        # sensor_beta: shared columns only.
        cur.execute(
            "CREATE TABLE sensor_beta ("
            "time TEXT, flag TEXT, counter1 INTEGER, counter2 INTEGER)")

        # Interleave the two tables by global index so the merged, timestamp
        # ordered stream is non-trivial. Even indices -> alpha, odd -> beta.
        alpha = []
        beta = []
        alpha_i = 0
        beta_i = 0
        for i in range(_ALPHA_ROWS + _BETA_ROWS):
            ts = _fmt(_timestamp(i))
            if i % 2 == 0 and alpha_i < _ALPHA_ROWS:
                alpha.append((ts, "on" if (alpha_i % 2 == 0) else "off",
                              10 + alpha_i, 100 + alpha_i * 2, 1000 + alpha_i))
                alpha_i += 1
            elif beta_i < _BETA_ROWS:
                beta.append((ts, "hi" if (beta_i % 2 == 0) else "lo",
                             20 + beta_i, 200 + beta_i * 3))
                beta_i += 1
            elif alpha_i < _ALPHA_ROWS:
                alpha.append((ts, "on" if (alpha_i % 2 == 0) else "off",
                              10 + alpha_i, 100 + alpha_i * 2, 1000 + alpha_i))
                alpha_i += 1

        cur.executemany(
            "INSERT INTO sensor_alpha (time, flag, counter1, counter2, counter3) "
            "VALUES (?, ?, ?, ?, ?)", alpha)
        cur.executemany(
            "INSERT INTO sensor_beta (time, flag, counter1, counter2) "
            "VALUES (?, ?, ?, ?)", beta)
        conn.commit()
    finally:
        conn.close()


def _epoch_nanos(dt):
    return int(dt.timestamp()) * 1_000_000_000 + dt.microsecond * 1000


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "synthetic_trace.sqlite")
    generate(out)
    total = _ALPHA_ROWS + _BETA_ROWS
    first = _epoch_nanos(_timestamp(0))
    last = _epoch_nanos(_timestamp(total - 1))
    print("Wrote {} ({} events)".format(out, total))
    print("EXPECTED_EVENTS = {}".format(total))
    print("FIRST_TIMESTAMP = {}L".format(first))
    print("LAST_TIMESTAMP  = {}L".format(last))


if __name__ == "__main__":
    main()
