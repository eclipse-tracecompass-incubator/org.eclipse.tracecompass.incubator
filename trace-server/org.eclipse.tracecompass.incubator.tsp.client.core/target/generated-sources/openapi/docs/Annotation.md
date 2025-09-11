

# Annotation

An annotation is used to mark an interesting area at a given time or time range

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**entryId** | **Long** | Entry&#39;s unique ID or -1 if annotation not associated with an entry |  |
|**style** | [**OutputElementStyle**](OutputElementStyle.md) |  |  [optional] |
|**label** | **String** | Text label of this annotation |  [optional] |
|**type** | [**TypeEnum**](#TypeEnum) | Type of annotation indicating its location |  |
|**time** | **Long** | Time of this annotation |  |
|**duration** | **Long** | Duration of this annotation |  |



## Enum: TypeEnum

| Name | Value |
|---- | -----|
| CHART | &quot;CHART&quot; |
| TREE | &quot;TREE&quot; |



