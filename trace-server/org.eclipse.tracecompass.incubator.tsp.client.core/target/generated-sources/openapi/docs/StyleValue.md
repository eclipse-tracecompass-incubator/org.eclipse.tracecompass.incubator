

# StyleValue

Supported types of a style value.

## oneOf schemas
* [Double](Double.md)
* [Integer](Integer.md)
* [String](String.md)

## Example
```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.model.StyleValue;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Double;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Integer;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.String;

public class Example {
    public static void main(String[] args) {
        StyleValue exampleStyleValue = new StyleValue();

        // create a new Double
        Double exampleDouble = new Double();
        // set StyleValue to Double
        exampleStyleValue.setActualInstance(exampleDouble);
        // to get back the Double set earlier
        Double testDouble = (Double) exampleStyleValue.getActualInstance();

        // create a new Integer
        Integer exampleInteger = new Integer();
        // set StyleValue to Integer
        exampleStyleValue.setActualInstance(exampleInteger);
        // to get back the Integer set earlier
        Integer testInteger = (Integer) exampleStyleValue.getActualInstance();

        // create a new String
        String exampleString = new String();
        // set StyleValue to String
        exampleStyleValue.setActualInstance(exampleString);
        // to get back the String set earlier
        String testString = (String) exampleStyleValue.getActualInstance();
    }
}
```


