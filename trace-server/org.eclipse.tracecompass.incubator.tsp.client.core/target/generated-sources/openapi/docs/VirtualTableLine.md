

# VirtualTableLine


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**tags** | **Integer** | Optional tags for the entire line. A bit mask to apply for tagging elements (e.g. table lines, states). This can be used by the server to indicate if a filter matches and what action to apply. Use 0 for no tags, 1 and 2 are reserved, 4 for &#39;BORDER&#39; and 8 for &#39;HIGHLIGHT&#39;. |  [optional] |
|**cells** | [**List&lt;VirtualTableCell&gt;**](VirtualTableCell.md) | The content of the cells for this line. This array matches the column ids returned above |  |
|**index** | **Long** | The index of this line in the virtual table |  |



