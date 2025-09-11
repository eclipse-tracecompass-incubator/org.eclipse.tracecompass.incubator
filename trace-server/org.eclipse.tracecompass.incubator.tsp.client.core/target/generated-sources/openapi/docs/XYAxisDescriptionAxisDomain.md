

# XYAxisDescriptionAxisDomain

Optional domain of values that this axis supports

## oneOf schemas
* [AxisDomainCategorical](AxisDomainCategorical.md)
* [AxisDomainRange](AxisDomainRange.md)

## Example
```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYAxisDescriptionAxisDomain;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.AxisDomainCategorical;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.AxisDomainRange;

public class Example {
    public static void main(String[] args) {
        XYAxisDescriptionAxisDomain exampleXYAxisDescriptionAxisDomain = new XYAxisDescriptionAxisDomain();

        // create a new AxisDomainCategorical
        AxisDomainCategorical exampleAxisDomainCategorical = new AxisDomainCategorical();
        // set XYAxisDescriptionAxisDomain to AxisDomainCategorical
        exampleXYAxisDescriptionAxisDomain.setActualInstance(exampleAxisDomainCategorical);
        // to get back the AxisDomainCategorical set earlier
        AxisDomainCategorical testAxisDomainCategorical = (AxisDomainCategorical) exampleXYAxisDescriptionAxisDomain.getActualInstance();

        // create a new AxisDomainRange
        AxisDomainRange exampleAxisDomainRange = new AxisDomainRange();
        // set XYAxisDescriptionAxisDomain to AxisDomainRange
        exampleXYAxisDescriptionAxisDomain.setActualInstance(exampleAxisDomainRange);
        // to get back the AxisDomainRange set earlier
        AxisDomainRange testAxisDomainRange = (AxisDomainRange) exampleXYAxisDescriptionAxisDomain.getActualInstance();
    }
}
```


