
# Execution Comparison User Guide

The Execution Comparison is an analysis tool that compares the execution time between two groups of traces. This feature is particularly useful when analyzing two runs of a trace — one that performs well and another that doesn't.

<img width="1334" alt="exec_comp" src="https://github.com/user-attachments/assets/b6b26d45-5803-4123-ba91-b211407edb6b">


## Structure of the Execution Comparison

The Execution Comparison is made up of 3 distinct sections:
1. Data Selection (Top)
2. Filtering Query (Middle)
3. Differential Flame Graph (Bottom)

## Setting Up the Comparison

To begin using the Execution Comparison:

1. Create an experiment in Trace Compass.
2. In the Data Selection Section, select the traces you want to compare by toggling them.
   - By default, this compares the traces over their entire time interval.

<img width="838" alt="group_selection" src="https://github.com/user-attachments/assets/92305ab3-777b-4aa8-a333-42a4b27c2895">


## Specifying a Time Interval

To compare traces within a specific time range, use one of these methods:

1. **Using The Event Density Graphs:** Select the region you want to compare in the Event Density Graphs using your mouse's left button.
2. **Manual time interval setting:** Modify the time range panels (From and To panels) under the Event Density Graphs.
<img width="1325" alt="time_panels" src="https://github.com/user-attachments/assets/53754b20-431c-4596-8438-fa8e1467d3bd">


## Understanding the Differential Flame Graph

Once you've selected your traces and time interval, a differential flame graph will appear at the bottom of the screen. This graph visualizes the difference in execution time between the selected traces.
<img width="1225" alt="diff_flame_graph" src="https://github.com/user-attachments/assets/9c165249-c3f5-407a-9561-555a950279a4">


### Color Coding

The colors in the differential flame graph represent the difference between the traces selected in Group B and those in Group A:

- **Red:** Functions take longer to execute in Group B compared to Group A
- **Blue:** Functions are faster in Group B than in Group A
- **White:** Little difference in execution time (< 10%)
- **More contrasting colors:** Bigger difference in execution time (> 50%)

### Percentages

The percentages shown on the differential flame graph indicate the magnitude of the difference in execution time between Group B and Group A for each function.

## Additional Features

### Filtering Query Section

Located between the Data Selection Section and the Differential Flame Graph, the Filtering Query Section allows for easy reproduction and sharing of experiments.

To use this feature:
1. Press the "Get filtering query" button.
2. A textual prompt will be generated.
3. This prompt can be shared to reproduce the exact same conditions used for the Execution Comparison.
<img width="500" alt="filtering_query" src="https://github.com/user-attachments/assets/50f33f1d-51c7-4ea7-b315-bfb8363bd2d1">

### Reset Time Interval

At any point, you can press the Reset Time Interval buttons to set the selected traces and time intervals to the initial state for Group A and Group B respectively.

## Best Practices

- Start by comparing a well-performing trace with a poorly performing one to quickly identify areas of significant difference.
- Use the time interval selection to focus on specific periods of interest, such as when performance issues are known to occur.
- Share the filtering query with team members to ensure everyone is analyzing the same data in the same way.

## Troubleshooting

If you encounter issues or unexpected results:
- Ensure that the traces in Group A and Group B are comparable (e.g., from the same system or application).
- Check that the selected time intervals for both groups cover the same period of interest.
- If the differential flame graph appears blank, try adjusting the time interval or selecting different traces.
