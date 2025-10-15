

# SeriesModel

This model includes the series output style values.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**seriesId** | **Long** | Series&#39; ID |  |
|**seriesName** | **String** | Series&#39; name |  |
|**style** | [**OutputElementStyle**](OutputElementStyle.md) |  |  |
|**xValues** | **List&lt;Long&gt;** | X values as list of int64 values (e.g. timestamps). Example: [100, 200, 350]. Mutually exclusive with xCategories/xRanges. |  [optional] |
|**xCategories** | **List&lt;String&gt;** | X values as list of category strings. Example: [\&quot;READ\&quot;, \&quot;WRITE\&quot;]. Mutually exclusive with xValues/xRanges. |  [optional] |
|**xRanges** | [**List&lt;Range&gt;**](Range.md) | X values as list of start/end range objects. Example: [{\&quot;start\&quot;: 10, \&quot;end\&quot;: 20}, {\&quot;start\&quot;: 50, \&quot;end\&quot;: 75}]. Mutually exclusive with xValues/xCategories. |  [optional] |
|**yValues** | **List&lt;Double&gt;** | Series&#39; Y values |  |
|**xValuesDescription** | [**XYAxisDescription**](XYAxisDescription.md) |  |  |
|**yValuesDescription** | [**XYAxisDescription**](XYAxisDescription.md) |  |  |



