
import os
import re
from debian.debtags import output

event_name = ''
event_name_upper = event_name.upper()
event_name_upper_1 = event_name.upper()
event_name_lower = event_name_upper
events = {}
data = """private static final ITmfEventType GO{0} = new TmfEventType("{1}", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0,null )); //$NON-NLS-1$
    /**
     * Event{2} handler
     *
     * @param rank the rank of the event
     * @param reader the file to read
     * @return the event
     * @throws IOException the event could not be read
     */
    private ITmfEvent generateGo{3}(long rank, DataInput reader) throws IOException {{
    """
data_leb = " long {} = LEB128Util.read(reader);\n"
data_string = " String {} = reader.readUTF();\n"
data_end = "  return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GO{}, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, "
        
data_end_leb = "new TmfEventField({}, {}, null)"
       
regex = re.compile(r'\s+\S+:\s+{\"(\S+)\", (\S+), (.*), (.*)},')
string_strip_re = re.compile(r'\[\]string{(.*)}')
handlers = '' 
init = ''

with open('EventTypes.txt') as file:
    event_number = 0
    for line in file.readlines():
        print(line)
        result = regex.match(line)
        event_name = result.group(1)
        event_name_upper = event_name.upper()
        has_stack = result.group(2) == 'true'
        leb_args = result.group(3)
        has_leb = string_strip_re.match(leb_args)
        string_args = result.group(4)
        has_string = string_strip_re.match(string_args)
        lebs = []
        if has_leb:
            lebs = has_leb.group(1).split(',')
        strings = []
        if has_string:
            strings = has_string.group(1).split(',')
        handlers += (data.format(event_name_upper, event_name , event_name, event_name))
        if has_stack:
            handlers += data_leb.format('stackID')
        for var_name in lebs:
            handlers += data_leb.format(var_name.replace('"', ''))
        for var_name in strings:
            handlers += data_string.format(var_name.replace('"', ''))
        handlers += data_end.format(event_name_upper)
        args = []
        if has_stack:
            args.append(data_end_leb.format('"StackID"', 'stackID'))
        for var_name in lebs:
            args.append(data_end_leb.format(var_name, var_name.replace('"', '')))
        for var_name in strings:
            args.append(data_end_leb.format(var_name, var_name.replace('"', '')))
        if args:
            handlers += 'Arrays.asList(' + ', '.join(args) + ').toArray(new ITmfEventField[0])));'
        else:
            handlers += 'null));'
        handlers += '\n}\n'
        events[event_number] = 'generateGo{}'.format(event_name)
        event_number += 1

for event in events:
    init += ('fHandlers[{}]=(this::{});\n'.format(event, events[event]))

with open('out.java', 'w') as output:
    output.write(init)
    output.write('\n\n\n')
    output.write(handlers)
