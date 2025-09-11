

# RequestedFilterQueryParameters

FilterQueryParameters is used to support search and filter expressions for timegraph views

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**filterExpressionsMap** | **Map&lt;String, List&lt;String&gt;&gt;** | The key of this map can be \&quot;1\&quot; (means DIMMED) or \&quot;4\&quot; (means EXCLUDED) and the value is an array of the desired search query (e.g. {\&quot;1\&quot;: [\&quot;openat\&quot;, \&quot;duration&gt;10ms\&quot;]}) |  |
|**strategy** | [**StrategyEnum**](#StrategyEnum) | Optional parameter that enables the full search (deep search) or not |  [optional] |



## Enum: StrategyEnum

| Name | Value |
|---- | -----|
| SAMPLED | &quot;SAMPLED&quot; |
| DEEP | &quot;DEEP&quot; |



