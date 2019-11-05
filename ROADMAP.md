# Enhance modularity
The product brings modularity at packaging level via the separation between the product core (the engine) and its addons.
Both are packaged separately and then have their own source life cycle and versioning.

The aim is to enhance the modularity of the product so that anyone can easily add an input / output connector to the product core itself.
To reach this goal, the possibility to re-write the product with a framework based-on the OSGI technology is studied. 

The Spring project [SpringDM](#https://docs.spring.io/spring-osgi/docs/current/reference/html/) or the Apache project [Karaf](#https://karaf.apache.org/index.html) would be potential solutions.  