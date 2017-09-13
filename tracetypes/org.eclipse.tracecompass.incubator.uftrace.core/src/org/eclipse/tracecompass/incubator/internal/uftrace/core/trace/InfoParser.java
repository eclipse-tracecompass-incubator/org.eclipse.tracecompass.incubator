/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.trace;

/**
 * Metadata:
 * <ul>
 * <li>The metadata starts with a 8-byte magic string which is 0x46 0x74 0x72
 * 0x61 0x63 0x65 0x21 0x00 or "Ftrace!".</li>
 * <li>It's followed by a 4-byte number of file version and the current version
 * is 4.</li>
 * <li>And then there's a 2-byte number of header (metadata) size and the
 * current value is 40 (or 0x28).</li>
 * <li>The next byte identifies a byte-order (endian) in the data files. The
 * value is same as the ELF format (EI_DATA: 1 is for the little-endian and 2 is
 * for the big-endian).</li>
 * <li>The next byte tells the size of address or long int type also same as the
 * ELF format (EI_CLASS: 1 is for 32-bit and 2 is for 64-bit).</li>
 * <li>Then 64-bit bit mask (feat_mask) of enabled features comes after it. The
 * bit 0 is for PLT (library call) hooking, the bit 1 is for task and session
 * info, the bit 2 is for kernel tracing, the bit 3 is for function arguments,
 * the bit 4 is for function return value, the bit 5 is for whether symbol file
 * contains relative offset or absolute address, and the bit 6 is for max
 * (function) stack depth.</li>
 * <li>The next 64-bit mask (info_mask) is for which kind of process and system
 * information was saved after the metadata.</li>
 * <li>And then it followed by a 2-byte number of maximum function call (stack)
 * depth given by user.</li>
 * <li>The rest 6-byte is reserved for future use and should be filled with
 * zero.</li>
 * </ul>
 * After the metadata, info string follows in a "key:value" form.
 *
 * @author Matthew Khouzam
 *
 */
public class InfoParser {
    // TODO fill me
}
