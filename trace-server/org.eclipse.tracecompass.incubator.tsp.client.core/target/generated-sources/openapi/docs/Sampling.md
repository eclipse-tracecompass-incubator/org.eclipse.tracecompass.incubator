

# Sampling

Sampling values

## oneOf schemas
* [CategorySampling](CategorySampling.md)
* [RangeSampling](RangeSampling.md)
* [TimestampSampling](TimestampSampling.md)

## Example
```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Sampling;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.CategorySampling;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.RangeSampling;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimestampSampling;

public class Example {
    public static void main(String[] args) {
        Sampling exampleSampling = new Sampling();

        // create a new CategorySampling
        CategorySampling exampleCategorySampling = new CategorySampling();
        // set Sampling to CategorySampling
        exampleSampling.setActualInstance(exampleCategorySampling);
        // to get back the CategorySampling set earlier
        CategorySampling testCategorySampling = (CategorySampling) exampleSampling.getActualInstance();

        // create a new RangeSampling
        RangeSampling exampleRangeSampling = new RangeSampling();
        // set Sampling to RangeSampling
        exampleSampling.setActualInstance(exampleRangeSampling);
        // to get back the RangeSampling set earlier
        RangeSampling testRangeSampling = (RangeSampling) exampleSampling.getActualInstance();

        // create a new TimestampSampling
        TimestampSampling exampleTimestampSampling = new TimestampSampling();
        // set Sampling to TimestampSampling
        exampleSampling.setActualInstance(exampleTimestampSampling);
        // to get back the TimestampSampling set earlier
        TimestampSampling testTimestampSampling = (TimestampSampling) exampleSampling.getActualInstance();
    }
}
```


