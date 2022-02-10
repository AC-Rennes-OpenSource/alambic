# Initialization of the random service

This product offers multiple randomized data generators. The generators dealing with a person identity and address require the product store (e.g. Postgresql)
to be filled with dictionaries.

The file **_alambic-random-dictionary-sql-format.tar.gz_** contains the _sql_ statement files to perform this load operation over all dictionaries.

* alambic-random-dictionary-address-city.sql
* alambic-random-dictionary-address-street-label.sql
* alambic-random-dictionary-address-street-type.sql
* alambic-random-dictionary-identity-firstname-female.sql
* alambic-random-dictionary-identity-firstname-male.sql
* alambic-random-dictionary-identity-last-name.sql

## Example of command to import the dictionaries

`$ psql -h <database host (e.g. 'localhost')> -p 5432 -U <Alambic user (e.g. 'etl')> -d <database name (e.g. 'etl_database')> < <path to the file with sql statments>.sql`

### Example

`$ psql -h localhost -p 5432 -U etl -d etl_database' < alambic-random-dictionary-address-city.sql`

## Creating a new dictionary to load in database

It is possible to create a new dictionary _sql_ file from any text file containing a liste of values, line by line.

Execute the followwing command line :

`$ ./alambic-random-dictionary-sql-format.tar.gz <the input text file> <target sql file> <the dictionary item type>`


The supported dictionary item types are :

|NAME|DESCRIPTION|
| -- | --------- |
| ADDRESS_CITY | A city name |
| ADDRESS_LABEL | A street name |
| ADDRESS_TYPE | A street type (e.g. road, street, path, corner...) |
| FIRSTNAME_FEMALE | A female given name |
| FIRSTNAME_MALE | A male given name |
| LASTNAME | A surname |

### Example

`$ ./alambic-random-dictionary-sql-format.tar.gz my-fake-city-names.txt alambic-random-dictionary-address-city.sql ADDRESS_CITY`

## Creating a new tiny dictionary to load in unit tests

It is a good practice to run unit tests using lightweight dictionary data. The unit tests Java file [RandomUserGeneratorTest.java](https://github.com/AC-Rennes-OpenSource/alambic/blob/master/product/src/test/java/fr/gouv/education/acrennes/alambic/random/service/RandomUserGeneratorTest.java) is an example.

Go and see the following method snippet that illustrates how to load such JSON format dictionary :

`
private void setUpPersistenceData() throws AlambicException, JsonProcessingException, IOException {
...
}
`

To produce a small dictionary to load in the embedded H2 database, as depicted by [RandomUserGeneratorTest.java](https://github.com/AC-Rennes-OpenSource/alambic/blob/master/product/src/test/java/fr/gouv/education/acrennes/alambic/random/service/RandomUserGeneratorTest.java), run the following command :

`$ ./alambic-random-dictionary-json-format.tar.gz <the input text file> <target json file> <the dictionary item type>`

Usage is the same as for _sql_ treatments.
