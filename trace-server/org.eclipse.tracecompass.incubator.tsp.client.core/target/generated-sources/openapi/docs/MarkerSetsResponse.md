

# MarkerSetsResponse


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**statusMessage** | **String** |  |  |
|**status** | [**StatusEnum**](#StatusEnum) | All possible statuses for a server response |  |
|**model** | [**List&lt;MarkerSet&gt;**](MarkerSet.md) |  |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| RUNNING | &quot;RUNNING&quot; |
| COMPLETED | &quot;COMPLETED&quot; |
| FAILED | &quot;FAILED&quot; |
| CANCELLED | &quot;CANCELLED&quot; |



