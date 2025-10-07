

# XYAxisDescription

Describes a single axis in an XY chart, including label, unit, data type, and optional domain.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**dataType** | [**DataTypeEnum**](#DataTypeEnum) | The type of data this axis represents |  |
|**unit** | **String** | Unit associated with this axis (e.g., ns, ms) |  |
|**label** | **String** | Label for the axis |  |
|**axisDomain** | [**XYAxisDescriptionAxisDomain**](XYAxisDescriptionAxisDomain.md) |  |  [optional] |



## Enum: DataTypeEnum

| Name | Value |
|---- | -----|
| NUMBER | &quot;NUMBER&quot; |
| BINARY_NUMBER | &quot;BINARY_NUMBER&quot; |
| TIMESTAMP | &quot;TIMESTAMP&quot; |
| DURATION | &quot;DURATION&quot; |
| STRING | &quot;STRING&quot; |
| TIME_RANGE | &quot;TIME_RANGE&quot; |



