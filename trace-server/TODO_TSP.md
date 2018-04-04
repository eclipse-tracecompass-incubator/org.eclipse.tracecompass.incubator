# List of tasks TO DO for the trace server to conform to the TSP API:

* Bookmarks: Currently all the bookmarks classes in TC are in UI packages
* Features: List supported trace types and suppported outputs
* Symbol provider management
* Modify an experiment: Issue is that the experiments are looked up via UUID, however experiment UUIDS are computed from the encapsulted trace UUIDs, so modifying them would change the experiment UUID
* Upload a trace to a server
* List the outputs for a trace
* XY tooltips: there isn't really such an API in data providers