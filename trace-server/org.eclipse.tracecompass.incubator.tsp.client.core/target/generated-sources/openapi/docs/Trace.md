

# Trace

Trace model

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**end** | **Long** | The trace&#39;s end time |  |
|**nbEvents** | **Long** | Current number of indexed events in the trace |  |
|**indexingStatus** | [**IndexingStatusEnum**](#IndexingStatusEnum) | Status of the trace indexing |  |
|**name** | **String** | User defined name for the trace |  |
|**properties** | **Map&lt;String, String&gt;** | The trace&#39;s properties |  |
|**path** | **String** | Path to the trace on the server&#39;s file system |  |
|**start** | **Long** | The trace&#39;s start time |  |
|**uuid** | **UUID** | The trace&#39;s unique identifier |  |



## Enum: IndexingStatusEnum

| Name | Value |
|---- | -----|
| RUNNING | &quot;RUNNING&quot; |
| COMPLETED | &quot;COMPLETED&quot; |
| CLOSED | &quot;CLOSED&quot; |



