

# MetadataValue

Supported types of a metadata value. Only values of type Number or String are allowed.

## oneOf schemas
* [BigDecimal](BigDecimal.md)
* [String](String.md)

## Example
```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.model.MetadataValue;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.BigDecimal;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.String;

public class Example {
    public static void main(String[] args) {
        MetadataValue exampleMetadataValue = new MetadataValue();

        // create a new BigDecimal
        BigDecimal exampleBigDecimal = new BigDecimal();
        // set MetadataValue to BigDecimal
        exampleMetadataValue.setActualInstance(exampleBigDecimal);
        // to get back the BigDecimal set earlier
        BigDecimal testBigDecimal = (BigDecimal) exampleMetadataValue.getActualInstance();

        // create a new String
        String exampleString = new String();
        // set MetadataValue to String
        exampleMetadataValue.setActualInstance(exampleString);
        // to get back the String set earlier
        String testString = (String) exampleMetadataValue.getActualInstance();
    }
}
```


