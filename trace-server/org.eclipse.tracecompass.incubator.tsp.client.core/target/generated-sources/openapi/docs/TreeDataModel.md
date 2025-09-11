

# TreeDataModel

Base entry returned by tree endpoints

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**hasData** | **Boolean** | Whether or not this entry has data. false if absent. |  [optional] |
|**parentId** | **Long** | Optional unique ID to identify this entry&#39;s parent. If the parent ID is -1 or omitted, this entry has no parent. |  [optional] |
|**style** | [**OutputElementStyle**](OutputElementStyle.md) |  |  [optional] |
|**id** | **Long** | Unique ID to identify this entry in the backend |  |
|**labels** | **List&lt;String&gt;** | Array of cell labels to be displayed. The length of the array and the index of each column need to correspond to the header array returned in the tree model. |  |



