

# ConfigurationSourceType


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**parameterDescriptors** | [**List&lt;ConfigurationParameterDescriptor&gt;**](ConfigurationParameterDescriptor.md) | A list of configuration parameter descriptors to be passed when creating or updating a configuration instance of this type. Use this instead of schema. Omit if not used. |  [optional] |
|**description** | **String** | Optional, describes the configuration source type |  [optional] |
|**name** | **String** | The human readable name |  |
|**id** | **String** | The unique ID of the configuration source type |  |
|**schema** | **Object** | A JSON object that describes a JSON schema for parameters that the front-end needs to provide with corresponding values. The schema has to adhere to JSON schema specification (see https://json-schema.org/). Use this for complex parameter descriptions instead of parameterDescriptors. Omit if not used. |  [optional] |



