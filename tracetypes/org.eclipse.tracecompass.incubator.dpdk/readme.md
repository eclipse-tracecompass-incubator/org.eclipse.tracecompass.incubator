# Trace Compass DPDK plug-in

## Features

- DPDK traces which handles trace that are generate by the DPDK trace module
- Logical core analysis for logical core events in DPDK

## How to use

- Run DPDK programs with the `--trace` option specifying what tracepoints to use. This option can be added multiple times For example: `--trace="lib.eal.thread*" --trace="lib.eal.service*"`
- Open the resulting trace in Trace Compass with the DPDK incubator plug-in