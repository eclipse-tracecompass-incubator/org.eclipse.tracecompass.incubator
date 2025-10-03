

# TimeGraphTooltipResponse


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**statusMessage** | **String** |  |  |
|**status** | [**StatusEnum**](#StatusEnum) | All possible statuses for a server response |  |
|**model** | **Map&lt;String, String&gt;** | Tooltip map with key-value pairs, where the key is the tooltip name and the corresponding value is the tooltip value |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| RUNNING | &quot;RUNNING&quot; |
| COMPLETED | &quot;COMPLETED&quot; |
| FAILED | &quot;FAILED&quot; |
| CANCELLED | &quot;CANCELLED&quot; |



