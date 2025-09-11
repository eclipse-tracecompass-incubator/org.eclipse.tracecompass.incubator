

# OutputElementStyle

Represents the style on an element (ex. Entry, TimeGraphState, ...) returned by any output. Supports style inheritance. To avoid having too many styles, the element style can have a parent style and will have all the same style property values as the parent, and can add or override style properties.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**parentKey** | **String** | Optional, parent style key. If omitted there is no parent. The parent key should match a style key defined in the style model and is used for style inheritance. A comma-delimited list of parent style keys can be used for style composition, the last one taking precedence. |  [optional] |
|**values** | [**Map&lt;String, StyleValue&gt;**](StyleValue.md) | Style values or empty map if there are no values. Keys and values are defined in https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/blob/master/tmf/org.eclipse.tracecompass.tmf.core/src/org/eclipse/tracecompass/tmf/core/model/StyleProperties.java |  |



