

# DataProvider


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**parentId** | **String** | Optional parent Id for grouping purposes for example of derived data providers. |  [optional] |
|**description** | **String** | Describes the output provider&#39;s features |  |
|**name** | **String** | The human readable name |  |
|**id** | **String** | The output provider&#39;s ID |  |
|**type** | [**TypeEnum**](#TypeEnum) | Type of data returned by this output. Serves as a hint to determine what kind of view should be used for this output (ex. XY, Time Graph, Table, Gantt chart, etc..). Providers of type TREE_TIME_XY and TIME_GRAPH can be grouped under the same time axis. Providers of type DATA_TREE only provide a tree with columns and don&#39;t have any XY nor time graph data associated with it. Providers of type GANTT_CHART use the same endpoint as TIME_GRAPH, but have a different x-axis (duration, page faults, etc.), with their own separate ranges. Providers of type TREE_GENERIC_XY supports XY view with non-time x-axis. Providers of type NONE have no data to visualize. Can be used for grouping purposes and/or as data provider configurator. |  |
|**_configuration** | [**ModelConfiguration**](ModelConfiguration.md) |  |  [optional] |
|**capabilities** | [**OutputCapabilities**](OutputCapabilities.md) |  |  [optional] |



## Enum: TypeEnum

| Name | Value |
|---- | -----|
| TABLE | &quot;TABLE&quot; |
| TREE_TIME_XY | &quot;TREE_TIME_XY&quot; |
| TIME_GRAPH | &quot;TIME_GRAPH&quot; |
| DATA_TREE | &quot;DATA_TREE&quot; |
| NONE | &quot;NONE&quot; |
| GANTT_CHART | &quot;GANTT_CHART&quot; |
| TREE_GENERIC_XY | &quot;TREE_GENERIC_XY&quot; |



