

# ConfigurationParameterDescriptor

A list of configuration parameter descriptors to be passed when creating or updating a configuration instance of this type. Use this instead of schema. Omit if not used.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**dataType** | **String** | Optional data type hint of the configuration parameter. For example, use NUMBER for numbers, or STRING as strings. If omitted assume the default value is STRING. |  [optional] |
|**keyName** | **String** | The unique key name of the configuration parameter |  |
|**required** | **Boolean** | Optional flag indicating whether the configuration parameter is required or not. If ommitted the default value is false. |  [optional] |
|**description** | **String** | Optional, describes the configuration parameter |  [optional] |



