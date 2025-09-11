

# Experiment

Experiment model

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**traces** | [**List&lt;Trace&gt;**](Trace.md) | The traces encapsulated by this experiment |  |
|**end** | **Long** | The experiment&#39;s end time |  |
|**nbEvents** | **Long** | Current number of indexed events in the experiment |  |
|**indexingStatus** | [**IndexingStatusEnum**](#IndexingStatusEnum) | Status of the experiment indexing |  |
|**name** | **String** | User defined name for the experiment |  |
|**start** | **Long** | The experiment&#39;s start time |  |
|**uuid** | **UUID** | The experiment&#39;s unique identifier |  |



## Enum: IndexingStatusEnum

| Name | Value |
|---- | -----|
| RUNNING | &quot;RUNNING&quot; |
| COMPLETED | &quot;COMPLETED&quot; |
| CLOSED | &quot;CLOSED&quot; |



