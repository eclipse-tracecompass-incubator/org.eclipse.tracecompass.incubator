#!/usr/bin/env python3
# Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
# All rights reserved. This program and the accompanying materials are
# made available under the terms of the Eclipse Public License 2.0.
# SPDX-License-Identifier: EPL-2.0

"""Capture legacy TML output from the pinned upstream source without the new parser.

The temporary portability adapter preserves Windows word widths on LP64 hosts,
removes managed UI includes, and records raw timer words before legacy masking.
Parsing, ordering, direction inference, and synthetic-event insertion are intact.
The copied source retains its original copyright and MIT license header.
"""

import argparse
import hashlib
import json
import subprocess
import tempfile
from pathlib import Path

REVISION = "b1af4decb38d48958ce3fa5c70d403f0d48aa25b"
SOURCE_HASHES = {'tml_library.h': '6c320ba2217c24a173c13ee202a9c6bad7af67dfbca7e40948cb94a4c6369770', 'tml_library.cpp': 'bba97aea0658295e92c5c741843df333c58cb12db92638c2721241e7a202a235'}
WRAPPER = r'''
#include <iostream>
#include <iomanip>
#include "tml_library.h"
extern TML_OBJECT *tml_object_array;
extern TML_EVENT *tml_event_array;
extern uint32_t tml_header_trace_id;
extern uint32_t tml_header_timer_valid_mask;
int main(int argc, char **argv) {
    if (argc != 2) return 2;
    FILE *input = fopen(argv[1], "rb");
    if (!input) return 3;
    uint32_t threads, objects, events;
    int64_t ticks;
    char *error = nullptr;
    int status = tml_initialize(input, &threads, &objects, &events, &ticks, &error);
    fclose(input);
    if (status) { std::cerr << status << " " << (error ? error : "") << "\n"; return 4; }
    std::cout << "{\"event_count\":" << events << ",\"registry\":[";
    for (uint32_t i = 0; i < objects; i++) {
        TML_OBJECT &o = tml_object_array[i];
        if (i) std::cout << ",";
        std::cout << "{\"type\":" << o.tml_object_type << ",\"address\":" << o.tml_object_address
                  << ",\"parameter1\":" << o.tml_object_parameter_1 << ",\"parameter2\":" << o.tml_object_parameter_2
                  << ",\"name\":" << std::quoted(o.tml_object_name) << "}";
    }
    std::cout << "],\"events\":[";
    for (uint32_t i = 0; i < events; i++) {
        TML_EVENT &e = tml_event_array[i];
        if (i) std::cout << ",";
        std::cout << "{\"source_record\":" << e.tml_event_source_record << ",\"raw_timestamp\":" << (e.tml_event_source_record ? e.tml_event_raw_timestamp : 0) << ",\"rank\":" << i << ",\"context\":" << e.tml_event_context
                  << ",\"raw_priority\":" << e.tml_event_raw_priority << ",\"raw_event_id\":" << e.tml_event_raw_id
                  << ",\"event_id\":" << e.tml_event_id << ",\"masked_timestamp\":" << e.tml_event_time_stamp
                  << ",\"relative_ticks\":" << e.tml_event_relative_ticks << ",\"info\":["
                  << e.tml_event_info_1 << "," << e.tml_event_info_2 << "," << e.tml_event_info_3 << "," << e.tml_event_info_4 << "]}";
    }
    std::cout << "]}\n";
    tml_uninitialize();
    return 0;
}
'''

def capture(source, trace):
    """Adapt the exact legacy source in a temporary directory and capture JSON."""
    actual = subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip()
    if actual != REVISION:
        raise ValueError("Use the documented pinned TraceX source revision")
    source_dir = source / "src/ELTMLManaged"
    with tempfile.TemporaryDirectory(prefix="legacy-capture-") as temporary:
        output = Path(temporary)
        for name in ("tml_library.h", "tml_library.cpp"):
            original = (source_dir / name).read_bytes()
            if hashlib.sha256(original).hexdigest() != SOURCE_HASHES[name]:
                raise ValueError("Legacy source content differs: " + name)
            value = original.decode("utf-8-sig")
            value = value.replace('#include "stdafx.h"', '').replace('#include "ELTMLManaged.h"', '')
            value = value.replace("unsigned long long", "uint64_t").replace("unsigned long", "uint32_t")
            value = value.replace("_int64", "int64_t").replace("ULONG_MAX", "UINT32_MAX")
            if name.endswith(".h"):
                value = value.replace("uint32_t       tml_event_raw_id;", "uint32_t       tml_event_raw_timestamp;\n    uint32_t       tml_event_source_record;\n    uint32_t       tml_event_raw_id;")
            else:
                value = value.replace("working_event_array[j].tml_event_raw_id =", "working_event_array[j].tml_event_raw_timestamp = working_event_array[j].tml_event_time_stamp;\n        working_event_array[j].tml_event_source_record = 1;\n        working_event_array[j].tml_event_raw_id =")
                value = value.replace("new_event_array[j].tml_event_id =", "new_event_array[j].tml_event_source_record = 0;\n                new_event_array[j].tml_event_id =")
            (output / name).write_text("#include <cstdint>\n#include <cstring>\n#include <climits>\n" + value)
        (output / "capture.cpp").write_text(WRAPPER)
        subprocess.run(["g++", "-std=c++14", "-w", str(output / "capture.cpp"), str(output / "tml_library.cpp"), "-o", str(output / "capture")], check=True)
        result = json.loads(subprocess.check_output([str(output / "capture"), str(trace.resolve())], text=True))
        raw = trace.read_bytes()
        order = "little" if raw[:4] == b"BTXT" else "big"
        mask = int.from_bytes(raw[4:8], order)
        result["byte_order"] = order.upper() + "_ENDIAN"
        result["timer_mask"] = mask
        originals = [e for e in result["events"] if e["source_record"]]
        increases = sum((b["masked_timestamp"] - a["masked_timestamp"]) & mask
                        for a, b in zip(originals, originals[1:]))
        decreases = sum((a["masked_timestamp"] - b["masked_timestamp"]) & mask
                        for a, b in zip(originals, originals[1:]))
        elapsed = originals[-1]["relative_ticks"] if originals else 0
        if elapsed == increases:
            result["timer_direction"] = "increasing"
        elif elapsed == decreases:
            result["timer_direction"] = "decreasing"
        else:
            raise ValueError("Legacy elapsed ticks do not match either modular direction")
        # Synthetic records have no input raw ID, priority, or timer word.
        for event in result["events"]:
            if not event["source_record"]:
                for field in ("raw_event_id", "raw_priority", "raw_timestamp"):
                    event.pop(field, None)
        return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("trace", type=Path)
    args = parser.parse_args()
    print(json.dumps(capture(args.source, args.trace), indent=2))
