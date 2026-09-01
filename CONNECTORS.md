This document lists the supported Alambic connectors and details the way to configure them within a job's definition file (XML format).

**TO NOTICE ABOUT SOURCES :**
- the query configured by the element _&lt;query&gt;_ will be run when the job definition is loaded.
- the result set can be iterated at runtime by a Freemarker directive ***Fn.getEntries()***.

1. [Source connectors](#source-connectors)
	1. [None](#none)
	2. [LDAP](#ldap)
	3. [Relational database](#relational-database)
	4. [Web service](#web-service)
	5. [XML](#xml)
	6. [Nuxeo-NXQL](#nuxeo-nxql)
	7. [File explorer](#file-explorer)
	8. [Grep](#grep)
	9. [CSV](#csv)
	10. [BaseX](#basex)
	11. [Random generators](#random-generators)
		1. [Date](#date)
		2. [Integer](#integer)
		3. [Fake user](#fake-user)
		4. [UID](#uid)
		5. [UUID](#uuid)
2. [Target connectors](#target-connectors)
	1. [LDAP](#ldap-1)
	2. [LDAP delete](#ldap-delete)
	3. [Relational database](#relational-database-1)
	4. [Web service](#web-service-1)
	5. [File](#file)
	6. [CSV](#csv-1)
	7. [Cipher](#cipher)
	8. [Notification](#notification)
	9. [Nuxeo](#nuxeo)
	10. [Nuxeo operation chain](#nuxeo-operation-chain)
3. [Additional connectors capabilities](#additional-connectors-capabilities)
	1. [Dynamic call](#dynamic-call)
	2. [Paged result set](#paged-result-set)
	3. [Capabilities matrix](#capabilities-matrix)

# Source connectors
## None
This resource is useful when no source is required but the target connector defines itself an input XML file instead (go & see the target [file](#file)).

```xml
<resource type="none" name="{any string}"/>
```

---
## LDAP
To search entries into a LDAP server instance. 

```xml
<resource type="ldap" name="{any string}" 
          connectTimeout="{max expected time to establish a connection in millis (a non-negative integer value)}"
          readTimeout="{max expected time to read data in millis (a non-negative integer value)}">
  <driver>{a LDAP driver class (e.g. 'com.sun.jndi.ldap.LdapCtxFactory')}</driver>
  <uri>{a LDAP server access URL (e.g. 'ldap://***:389')}</uri>
  <login>{the user dn to bind with}</login>
  <passwd>{the user password to connect with}</passwd>
  <query>{the LDAP syntax query to run (e.g. (&amp;(objecClass=person)(givenName=joe)) )}</query>
  <attributeList>{the list of attributes to fetch from the query result set}</attributeList>
</resource>
```

**Example :** query a LDAP server
```xml
<resource type="ldap" name="My LDAP server" connectTimeout="5000" readTimeout="60000">
  <driver>com.sun.jndi.ldap.LdapCtxFactory</driver>
  <uri>ldap//localhost:389/ou=books</uri>
  <login>John</login>
  <passwd>Doe</passwd>
  <query>(&amp;(objectClass=books)(category=Thriller))</query>
  <attributeList>author,year,model,price</attributeList>
</resource>
```

---
## Relational database
To search tuples into a relational database.

```xml
<resource type="sql" name="{any string}">
  <driver>{a relational database driver class (e.g. 'org.postgresql.Driver')}</driver>
  <uri>{a relational database server access URL (e.g. 'jdbc://***:5432/database name')}</uri>
  <login>{the user name to connect with}</login>
  <passwd>{the user password to connect with}</passwd>
  <query>{the SQL syntax query to run (e.g. (SELECT b.title FROM books as b WHERE b.category = 'Thriller')}</query>
  <attributeList>{the list of attributes to fetch from the query result set}</attributeList>
</resource>
```

**Example :** query a database hosted by a PostgreSQL server
```xml
<resource type="sql" name="My SQL database">
  <driver>org.postgresql.Driver</driver>
  <uri>jdbc:postgresql://localhost:5432/digitallibrary</uri>
  <login>John</login>
  <passwd>Doe</passwd>
  <query>SELECT b.title FROM books as b WHERE b.category = 'Thriller'</query>
  <attributeList>author,year,model,price</attributeList>
</resource>
```

---
## Web service
To request a remote web service API.

```xml
<resource type="webService" name="{any string}" connectionTimeout="{the timeout to connect the remote API (in seconds). As default is 5 seconds}">
  <uri>{the HTTP REST API base URL (e.g. http://localhost:8778/api/v1/)}</uri>
  <method>{the HTTP request method (e.g. GET, POST, PUT, PATCH, DELETE...). As default, the GET method is used.}</method>
  <response_codes>
    <code type="success">{the HTTP response code that will be considered as successful. Use multiple XML element <code> to define multiple success codes. As default, the HTTP response code 200 is used to define whether an API run successfully}</code>
	...
  </response_codes>
  <headers>
    <header name="{the header name (e.g. to specify the payload 'Content-Type')}">{the header value (e.g. 'application/json', since version 2.0.9 you can include an expression with a variable like 'Bearer %MY_VARIABLE%')}<header>
    ...
  </headers>
  <query>{the API query with respect of a specific format and grammar (see the subsequent 'TO NOTICE')}</query>
  <proxy>
    <host>{the proxy hostname to use when calling the API ()}</host>
    <port>{the proxy port to use when calling the API}</port>
  </proxy>
  <authentication>
    <credentials>
      <login>{the user login to authenticate to the remote API}</login>
      <password>{the user password to authenticate to the remote API}</password>
    </credentials>
  </authentication>
</resource>
```

> **TO NOTICE :**
> - the following XML elements are optional : method, response_codes, headers, proxy, authentication.
> - the *query* element must fit the JSON format and respect the following grammar :
> ```json
> {
>   "api": "<the api suffix>",
>   "parameters": "<the API URL parameters>",
>   "payload": "<the API payload data (for requiring methods as POST, PUT...)>"
> }
> ``` 

**Example :** GET request to the API : http://the.digitallibrary.com/api/v1/books/_search?category='Thriller'&price=10.0
```xml
<resource type="webService" name="The digitallibrary API">
  <uri>http://the.digitallibrary.com/api/v1/</uri>
  <method>GET</method>
  <response_codes>
    <code type="success">200</code>
    <code type="success">404</code>
  </response_codes>
  <headers>
    <header name="Content-Type">application/json<header>
  </headers>
  <query>{"api": "books/_search", "parameters": "category='Thriller'&amp;price=10.0"}</query>
  <proxy/>
  <authentication>
    <credentials>
      <login>John</login>
      <password>Doe</password>
    </credentials>
  </authentication>
</resource>
```

**Example :** POSTrequest (with payload) to the API : http://the.digitallibrary.com/api/v1/books/_add
```xml
<resource type="webService" name="The digitallibrary API" connectionTimeout="2000">
  <uri>http://the.digitallibrary.com/api/v1/</uri>
  <method>POST</method>
  <response_codes>
    <code type="success">201</code>
  </response_codes>
  <headers>
    <header name="Content-Type">application/json<header>
  </headers>
  <query>{"api":"books/_add", "payload": {"title":"The green monster","author":"Jhon.Doe","category":"Thriller","price":12.5}}</query>
  <proxy/>
  <authentication>
    <credentials>
      <login>Jane</login>
      <password>Doe</password>
    </credentials>
  </authentication>
</resource>
```

---
## BaseX
To request a XML database server BaseX.

```xml
<resource type="basex" name="{any string}" connectionTimeout="{the timeout to connect the BaseX server (in seconds). As default is 5 seconds}">
  <host>{the BaseX server hostname}</host>
  <port>{the BaseX server HTTP port}</port>
  <database>{the BaseX XML database}</database>
  <query>{the database query (XPATH format)}</query>
  <proxy>
    <host>{the proxy hostname to use when calling the API ()}</host>
    <port>{the proxy port to use when calling the API}</port>
  </proxy>
  <authentication>
    <credentials>
      <login>{the user login to authenticate to the BaseX server}</login>
      <password>{the user password to authenticate to the BaseX server}</password>
    </credentials>
  </authentication>
</resource>
```

**Example :**
```xml
<resource type="basex" name="My BaseX server" connectionTimeout="3000">
  <host>http://localhost</host>
  <port>8984</port>
  <database>digitallibrary</database>
  <query>/books/book[@name='category' and value='Thriller']</query>
  <proxy>
    <host>http://proxy.inner.com</host>
    <port>2132</port>
  </proxy>
  <authentication>
    <credentials>
      <login>John</login>
      <password>Doe</password>
    </credentials>
  </authentication>
</resource>
```

---
## XML
To parse and search elements from an XML file. 

```xml
<resource type="xml" name="{any string}">
  <xml>{the full path of a XML type file}</xml>
  <xslt>{the full path of a XSLT template file}</xslt>
  <pivot>{the target XML file full path}</pivot>
</resource>
```

> **TO NOTICE :**
> - the template defined by the tag &lt;xslt&gt; wil be applied to each entry of the input file defined by the tag &lt;xml&gt;. 
> - The result is placed into the file defined by the tag &lt;pivot&gt; and may be used as the source of a job.

**Example :**
```xml
<resource type="xml" name="Books registry">
  <xml>/.../books/listing.xml</xml>
  <xslt>/.../books/template.xslt</xslt>
  <pivot>/.../books/registry.xml</pivot>
</resource>
```

---
## CSV
To parse and search elements from a CSV file.

```xml
<resource type="csv" name="{any string}">
  <input>{the full path of a CSV type file}</input>
  <separator>{the attributes separator. As default, the character ";" is used}</separator>
</resource>
```

---
## File explorer
To list the content of a file system directory.

```xml
<resource type="fileExplorer" name="{any string}">
  <query>{a file system listing query with respect of a specific format and grammar (see the subsequent 'TO NOTICE')}</query>
</resource>
```

> **TO NOTICE :**
> - the *query* element must fit the JSON format and respect the following grammar :
> ```json
> {
>   "rootPath": "<the root directory where to run the listing command/query>",
>   "filterRegex": "<the regular expression to use to filter the root directory content>"
> }
> ``` 

**Example :** list all book files from one directory
```xml
<resource type="fileExplorer" name="My library books">
  <query>{"rootPath":"/digitallibrary/books","filterRegex":"*.doc"}</query>
</resource>
```

---
## Grep
To search a lines matching a pattern string inside a text file.

```xml
<resource type="grep" name="{any string}">
  <input>{the text file to apply the search pattern on}</input>
  <query>{the regular expression pattern ('grep' like)}</query>
</resource>
```

**Example :** list all lines of a file dealing with a pattern
```xml
<resource type="grep" name="My library listing">
  <input>/digitallibrary/books/listing.txt</input>
  <query>.+category=(Thriller|Entertainment).+</query>
</resource>
```

---
## Nuxeo - NXQL
To search documents from a Nuxeo repository.

```xml
<resource type="nxql" name="{any string}">
  <uri>{a Nuxeo server access URL}</uri>
  <login>{the user name to connect with}</login>
  <passwd>{the user password to connect with}</passwd>
  <schemas>{the metadata schemas to fetch}</schemas>
  <query>{the NXQL syntax query to run}</query>
</resource>
```

**Example :** list all documents dealing with some metadata predicates
```xml
<resource type="nxql" name="My library ECM">
  <uri>http://localhost:8080/nuxeo/site/automation</uri>
  <login>John</login>
  <passwd>Doe</passwd>
  <schemas>dublincore, book</schemas>
  <query>SELECT * FROM Document WHERE ecm:primaryType = 'Book' AND dc:subjects IN ('Thriller') AND ecm:currentLifeCycleState != 'deleted'</query>
</resource>
```

---
## Random generators
### Date
To generate a random date within en range of years.

```xml
<resource type="randomDateGenerator" name="{any string}">
  <query>{a date query with respect of a specific format and grammar (see the subsequent 'TO NOTICE')}</query>
</resource>
```

> **TO NOTICE :**
> - the *query* element must fit the JSON format and respect the following grammar :
> ```json
> {
>   "count": "<an integer. the number of random dates to generate>",
>   "lowerYear": "<the lower interval from which to compute a random date>",
>   "upperYear": "<the upper interval from which to compute a random date>",
>   "processId": "<an arbitrary string to specify the query business context. a good practice is too generate a randomisation key for each usage (could be hash result + base64).>",
>   "blurid": "<a blur identifier for whom the random date(s) is(are) generated>",
>   "reuse": "<a boolean. do reuse or not a previously generated random date with same blur and process identifier>"
> }
> ``` 

**Example :** get one random date from scratch or from a previous call if any
```xml
<resource type="randomDateGenerator" name="My random generator">
  <query>{"count":1,"processId":"1FrtpoAA3ftjoV5se9=","reuse":"true", "blurid":"5213-55656577-20540398-7539","lowerYear":"1973","upperYear":"1984"}</query>
</resource>
```

---
### Integer
To generate a random integer within a range of lower and upper values.

```xml
<resource type="randomIntegerGenerator" name="{any string}">
  <query>{an integer query with respect of a specific format and grammar (see the subsequent 'TO NOTICE')}</query>
</resource>
```

> **TO NOTICE :**
> - the *query* element must fit the JSON format and respect the following grammar :
> ```json
> {
>   "count": "<an integer. the number of random dates to generate>",
>   "minValue": "<the lower interval from which to compute a random integer>",
>   "maxValue": "<the upper interval from which to compute a random integer>",
>   "processId": "<an arbitrary string to specify the query business context. a good practice is too generate a randomisation key for each usage (could be hash result + base64).>",
>   "blurid": "<a blur identifier for whom the random date(s) is(are) generated>",
>   "reuse": "<a boolean. do reuse or not a previously generated random date with same blur and process identifier>"
> }
> ``` 

**Example :** get one random date from scratch or from a previous call if any
```xml
<resource type="randomIntegerGenerator" name="My random generator">
  <query>{"count":1,"processId":"1Frtpo5se9=","reuse":"true", "blurid":"5213-55656577-20540398-7539","minValue":"1","minValue":"100"}</query>
</resource>
```

---
### Fake user
To obtain a random user identity.

```xml
<resource type="randomUserGenerator" name="{any string}">
  <query>{a fake user query with respect of a specific format and grammar (see the subsequent 'TO NOTICE')}</query>
</resource>
```

> **TO NOTICE :**
> - the *query* element must fit the JSON format and respect the following grammar :
> ```json
> {
>   "count": "<an integer. the number of random dates to generate>",
>   "gender": "<the fake user gender (either 'FEMAL' or 'MAL')>",
>   "processId": "<an arbitrary string to specify the query business context. a good practice is too generate a randomisation key for each usage (could be hash result + base64).>",
>   "blurid": "<a blur identifier for whom the random date(s) is(are) generated>",
>   "reuse": "<a boolean. do reuse or not a previously generated random date with same blur and process identifier>"
> }
> ``` 

**Example :** get one random fake user from scratch or from a previous call if any
```xml
<resource type="randomUserGenerator" name="My random generator">
  <query>{"count":1,"processId":"1Frtpo5se9=","reuse":"true", "blurid":"5213-55656577-20540398-7539","gender":"FEMAL"}</query>
</resource>
```

---
### UUID
To obtain a random Universal Unique IDentifier.

```xml
<resource type="randomUUidGenerator" name="{any string}">
  <query>{a query with respect of a specific format and grammar (see the subsequent 'TO NOTICE')}</query>
</resource>
```

> **TO NOTICE :**
> - the *query* element must fit the JSON format and respect the following grammar :
> ```json
> {
>   "count": "<an integer. the number of random dates to generate>",
>   "processId": "<an arbitrary string to specify the query business context. a good practice is too generate a randomisation key for each usage (could be hash result + base64).>",
>   "blurid": "<a blur identifier for whom the random date(s) is(are) generated>",
>   "reuse": "<a boolean. do reuse or not a previously generated random date with same blur and process identifier>"
> }
> ``` 

**Example :** get one random fake user from scratch or from a previous call if any
```xml
<resource type="randomUUidGenerator" name="My random generator">
  <query>{"count":1,"processId":"1Frtpo5se9=","reuse":"true", "blurid":"5213-55656577-20540398-7539"}</query>
</resource>
```

---
### UID
To build an unique user identifier based-on its first and last names.

```xml
<resource type="randomUidGenerator" name="{any string}">
  <query>{a unique identifier query with respect of a specific format and grammar (see the subsequent 'TO NOTICE')}</query>
</resource>
```

> **TO NOTICE :**
> - the *query* element must fit the JSON format and respect the following grammar :
> ```json
> {
>   "count": "<an integer. the number of random dates to generate>",
>   "firstName": "<the user's first name for whom to generate a unique identifier>",
>   "lastName": "<the user's last name for whom to generate a unique identifier>",
>   "format": "<the unique identifier format : either SHORT (first character of the first name appended to the last name + index. example 'jdoe4') or LONG (full first name + dot + last name + index. example 'john.doe7')>",
>   "processId": "<an arbitrary string to specify the query business context. a good practice is too generate a randomisation key for each usage (could be hash result + base64).>",
>   "blurid": "<a blur identifier for whom the random date(s) is(are) generated>",
>   "reuse": "<a boolean. do reuse or not a previously generated random date with same blur and process identifier>"
> }
> ``` 

**Example :** get one random fake user from scratch or from a previous call if any
```xml
<resource type="randomUidGenerator" name="My random generator">
  <query>{"count":1,"processId":"1Frtpo5se9=","reuse":"true", "blurid":"5213-55656577-20540398-7539", "firstName":"John", "lastName":"Doe", "format":"LONG"}</query>
</resource>
```

# Target connectors
## LDAP
To update the entries (CUD operations) from a LDAP server.

```xml
<destination type="ldap" name="{any string}"
             connectTimeout="{max expected time to establish a connection in millis (a non-negative integer value)}"
             readTimeout="{max expected time to read data in millis (a non-negative integer value)}">
  <driver>{a LDAP driver class (e.g. 'com.sun.jndi.ldap.LdapCtxFactory')}</driver>
  <uri>{a LDAP server access URL (e.g. 'ldap://***:389')}</uri>
  <login>{the user dn to bind with}</login>
  <passwd>{the user password to connect with}</passwd>
  <pivot>{the intermediate file (XML format) to load into the target LDAP server}</pivot>
</destination>
```

**Example :** load entries from an intermediate file into a LDAP server
```xml
<destination type="ldap" name="My LDAP server" connectTimeout="5000" readTimeout="60000">
  <driver>com.sun.jndi.ldap.LdapCtxFactory</driver>
  <uri>ldap//localhost:389/ou=books</uri>
  <login>John</login>
  <passwd>Doe</passwd>
  <pivot>/mydigitallibrary/books.xml</pivot>
</destination>
```

---
## LDAP delete
To delete entries from a LDAP server.

```xml
<destination type="ldapDelete" name="{any string}" dry="{a boolean. whether to really delete the entries or log them only. as default, 'false' : do delete for real}">
  <driver>{a LDAP driver class (e.g. 'com.sun.jndi.ldap.LdapCtxFactory')}</driver>
  <uri>{a LDAP server access URL (e.g. 'ldap://***:389')}</uri>
  <login>{the user dn to bind with}</login>
  <passwd>{the user password to connect with}</passwd>
  <rdnAttrName>{the LDAP entry attribute to use to retrieve the target LDAP entry to delete according to the the job's source inputs}</rdnAttrName>
</destination>
```

**Example :** delete the entries listed by a job's resource, based-on the attribute "id". Don't delete for real but only log the delete operations.
```xml
<destination type="ldapDelete" name="My LDAP server" dry="true">
  <driver>com.sun.jndi.ldap.LdapCtxFactory</driver>
  <uri>ldap//localhost:389/ou=books</uri>
  <login>John</login>
  <passwd>Doe</passwd>
  <rdnAttrName>id</rdnAttrName>
</destination>
```

---
## File
To generate a file based-on a Freemarker template.

```xml
<destination type="file" name="{any string}">
	<template>{the path of the Freemarker template file}</template>
	<output>{the path of the file resulting from the Freemarker template applied to the job's resource(s)}</output>
</destination>
```

**Example :** build an intermediate file to load a LDAP server.
```xml
<destination type="file" name="My intermediate file">
	<template>../resources/books-template.ftl</template>
	<output>../output/books.xml</output>
</destination>
```

---
## Cipher
To cipher a file.

```xml
<destination type="cipher" name="{any string}">
	<algorithm>{the ciphering algorithms. either 'AES' (symmetrical) or 'RSA' (asymmetrical)}</algorithm>
	<mode>{the ciphering mode. either 'ENCRYPT_MODE' or 'DECRYPT_MODE'}</mode>
	<key type="{the key type}">{the base64 string representation of the key}</key>
	<keystore type="{the keystore type. either 'JCEKS' (to store a mix of symmetrical and asymmetrical keys) or 'DEFAULT' (other usages)}">
		<path>{the path of the keystore containing the keys to cipher/uncipher}</path>
		<password>{the keystore password}</password>
		<key type="{the type of the key to use. either 'secret' (for symmetrical AES algorithm) or 'rsa' (for asymmetrical RSA algorithm)}">
			<alias>{the alias to get the key}</alias>
			<password>{the key password}</password>
		</key>
	</keystore>
	<input>{either the plain text file to cipher or ciphered file to uncipher}</input>
	<output>{the cipher function result file}</output>
</destination>
```

> **TO NOTICE :**
> - either the XML tag &lt;key&gt; or &lt;keystore&gt; must be used, not both at once.

**Example :** AES algorithm (128 bits key) - use a string representation of a key : cipher
```xml
<destination type="cipher" name="encode-file">
  <algorithm>AES</algorithm>
  <mode>ENCRYPT_MODE</mode>
  <key>fJMP...9joiQ==</key>
  <input>../output/plain-data.txt</input>
  <output>../output/ciphered-data.cip</output>
</destination>
```

**Example :** RSA algorithm (2048 bits key) - use a string representation of a key : uncipher
```xml
<destination type="cipher" name="decode-file">
  <algorithm>RSA</algorithm>
  <mode>DECRYPT_MODE</mode>
  <key type="private">MIICdQIBA...mstw6fNuOjnB</key>
  <input>../output/ciphered-data.cip</input>
  <output>../output/plain-data.txt</output>
</destination>
```

**Example :** AES algorithm (128 bits key) - use a keystore : cipher
```xml
<destination type="cipher" name="encode-file">
  <algorithm>AES</algorithm>
  <mode>ENCRYPT_MODE</mode>
  <keystore type="JCEKS">
    <path>../resources/dataloader.keystore</path>
    <password>password</password>
    <key type="secret">
      <alias>secretkeyaes128</alias>
      <password>keypassword</password>
    </key>
  </keystore>
  <input>../output/plain-data.txt</input>
  <output>../output/ciphered-data.cip<output>
</destination>
```

**Example :** RSA algorithm (1024 bits key) - use a keystore : uncipher
```xml
<destination type="cipher" name="decode-file">
  <algorithm>RSA</algorithm>
  <mode>DECRYPT_MODE</mode>
  <keystore type="JCEKS">
    <path>../resources/dataloader.keystore</path>
    <password>password</password>
    <key type="private">
      <alias>keypairrsa1024</alias>
      <password>keypassword</password>
    </key>
  </keystore>
  <input>../output/ciphered-data.enc</input>
  <output>../output/plain-data.txt</output>
</destination>
```

---
## Web service
To request a web service to update (CUD) resources.

```xml
<resource type="webService" name="{any string}" connectionTimeout="{the timeout to connect the remote API (in seconds). As default is 5 seconds}">
  <pivot>{the intermediate file containing the API specific definitions (API URL, headers, payload...)}</pivot>
  <proxy>
    <host>{the proxy hostname to use when calling the API ()}</host>
    <port>{the proxy port to use when calling the API}</port>
  </proxy>
  <authentication>
    <credentials>
      <login>{the user login to authenticate to the remote API}</login>
      <password>{the user password to authenticate to the remote API}</password>
    </credentials>
  </authentication>
</resource>
```

> **TO NOTICE :**
> - the following XML elements are optional : proxy, authentication.
> - the intermediate file pointed by the *pivot* element must fit the XML format and respect the following grammar :
> ```xml
> <?xml version="1.0" encoding="UTF-8"?>
> <restConnector>
>   <api uri="{the HTTP REST API base URL (e.g. http://localhost:8778/api/v1/)}" method="{the HTTP request method (e.g. GET, POST, PUT, PATCH, DELETE...). As default, the GET method is used.}">
>     <headers>
>       <header name="{the header name (e.g. to specify the payload 'Content-Type')}">{the header value (e.g. 'application/json')}</header>
>     </headers>
>     <response_codes>
>       <code type="success">{the HTTP response code that will be considered as successful. Use multiple XML element <code> to define multiple success codes. As default, the HTTP response code 200 is used to define whether an API run successfully}</code>
>       ...
>     </response_codes>
>     <payload>{the API specific payload}</payload>
>   </api>
>   <api uri="..." method="...">
>     ...
>   </api>
> </restConnector>
> ```

**Example :** intermediate file to modify aliases from an Elastic cluster
```xml
<?xml version="1.0" encoding="UTF-8"?>
<restConnector>
  <api uri="http://localhost:9200/_aliases" method="POST">
   <headers>
      <header name="Content-Type">application/json; charset=UTF-8</header>
    </headers>
    <response_codes>
      <code type="success">200</code>
    </response_codes>
    <payload>{ "actions" : [ { "remove" : { "alias" : "alias_books", "index" : "index_books_thriller_231019" } }, { "add" : { "alias" : "alias_books", "index" : "index_books_thriller_241019" } } ] }</payload>
  </api>
  <api uri="http://localhost:9200/_aliases" method="POST">
    <headers>
      <header name="Content-Type">application/json; charset=UTF-8</header>
    </headers>
    <response_codes>
      <code type="success">200</code>
    </response_codes>
    <payload>{ "actions" : [ { "remove" : { "alias" : "alias_books", "index" : "index_books_manga_231019" } }, { "add" : { "alias" : "alias_books", "index" : "index_books_manga_241019" } } ] }</payload>
  </api>
</restConnector>
```

---
## CSV
To generate a CSV file based-on the result set of a resource query.

```xml
<destination type="csv" name="{any string}">
  <path>{the target CSV file}</path>
  <format>{the list of attributes of the result set obtained from a query on the input job's resource. Each attribute name must be framed with the character "%"}</format>
</destination>
```

**Example :** fill a CSV file with the result of a source LDAP query
```xml
<destination type="csv" name="My target CSV file">
  <path>../output/ldap-query-result.csv</path>
  <format>%cn%;%givenName%;%sn%;%title%;%profile%</format>
</destination>
```
---
## Notification
To send email notifications.

```xml
<destination type="notification" name="{any string}">
  <mail.smtp.host>{the SMTP sender server hostname}</mail.smtp.host>
  <mail.smtp.port>{the SMTP sender server port (as default, is '25')}</mail.smtp.port>
    <attachments>
      <include id="{attachment name}">{attachment binary}</include>
    </attachments>
  <pivot>{the intermediate file containing the email body. Must respect the XML format and a particular grammar}</pivot>
</destination>
```

**Example :** send an email which body contains two images
```xml
<destination type="notification" name="SMTP">
  <mail.smtp.host>localhost</mail.smtp.host>
  <mail.smtp.port>25</mail.smtp.port>
  <attachments>
    <include id="logo">../resources/logo.jpg</include>
    <include id="marianne">../resources/marianne.jpg</include>
  </attachments>
  <pivot>../output/pivot-notifications.xml</pivot>
</destination>
```

**Example :** the intermediate file (_pivot-notifications.xml_) defining the notifications to send
```xml
<?xml version="1.0" encoding="UTF-8"?>
<alambic>
  <messages>
    <message>
      <from>
        <value>noreply@ac-rennes.fr</value>
      </from>
      <recipients>
        <item type="To">
          <value>john.doe@foo.com</value>
        </item>
        <item type="Cc">
          <value>anna.doe@foo.com</value>
        </item>
      </recipients>
      <subject>
        <value>Books availability</value>
      </subject>
      <content type="text/html; charset=utf-8">
        <value>(TEMPLATE){"path":"../resources/notification-template.html","civility":"Mr","firstName":"John","lastName":"Doe"}(/TEMPLATE)</value>
      </content>
      <log>
        <value>A notification was sent to Mr John Doe</value>
      </log>
      </message>
  </messages>
</alambic>
```

> **To NOTICE :**
> - the XML tag defines the log to produce when the notification is performed.

**Example :** the template file (_notification-template.html_) defining the email body
```html
<!DOCTYPE html>
<html>
  <head>
    <style>
#left-vertical-banner {
  width:18%;
  float:left;
  margin-right:25px
}

#right-top-banner {
  width:75%;
  float:left
}

#right-top-banner-marianne {
  margin-bottom: 70px;
}

#img-logo {
  max-width: 100%;
  max-height: 100%;	
}

#img-marianne {
  width: 15%;
  margin-left:30%;
  padding-top:30px
}
    </style>
  </head>
  <body>
    <div>
      <div id="left-vertical-banner">
        <img id="img-logo" src="cid:logo">
      </div>
      <div id="right-top-banner">
        <div id="right-top-banner-marianne">
          <img id="img-marianne" src="cid:marianne">
        </div>
        <div id="right-bottom-mail-content">
          <h3>Object : This is a notification for book availability</h3>
          <br/><br/>
          Dear ${civility} ${firstName} ${lastName},
          <br/><br/>
          we are happy to let you know that new books are available, Thriller category indeed!
          <br/><br/>
          Best Regards,<br/>
          The shop around the corner<br/><br/>
        </div>
      </div>
    </div>
  </body>
</html>
```

---
## Relational database
To update tuples (CUD) from a relational database. 

```xml
<destination type="sql" name="{any string}">
  <driver>{a LDAP driver class (e.g. 'com.sun.jndi.ldap.LdapCtxFactory')}</driver>
  <uri>{a LDAP server access URL (e.g. 'ldap://***:389')}</uri>
  <login>{the user dn to bind with}</login>
  <passwd>{the user password to connect with}</passwd>
  <pivot>{an intermediate file containing the SQL requests to execute}</pivot>
</destination>
```

**Example :** to execute SQL requests upon a database _books_
```xml
<destination type="sql" name="My target database">
  <driver>com.sun.jndi.ldap.LdapCtxFactory</driver>
  <uri>ldap//localhost:389/ou=books</uri>
  <login>John</login>
  <passwd>Doe</passwd>
  <pivot>my-intermediate-file.sql</pivot>
</destination>
```

**Example :** the intermediate file _my-intermediate-file.sql_
```sql
INSERT INTO books (title, category, author, date) VALUES ('Unsolved murders', 'Thriller', 'Emily G. Thompson', '05/02/2019');
INSERT INTO books (title, category, author, date) VALUES ('Close your eyes', 'Thriller', 'Iris & Roy Johansen', '17/12/2012');
INSERT INTO books (title, category, author, date) VALUES ('Knock, knock', 'Horror', 'S.P. Miskowski', '01/01/1970');
```

---
## Nuxeo
To update documents (CUD) from a Nuxeo repository.

```xml
<destination type="nuxeo" name="{any string}">
  <uri>{a Nuxeo server access URL}</uri>
  <login>{the user name to connect with}</login>
  <passwd>{the user password to connect with}</passwd>
  <input>{an intermediate file containing the Nuxeo directives to execute}</input>
  <path>{the Nuxeo server documents root path. Is used as base path for the documents referenced by the intermediate file}</path>
</destination>
```

**Example :** to create _book_ type documents within a Nuxeo server path _/mydigitallibrary/books_
```xml
<destination type="nuxeo" name="My target Nuxeo instance">
  <uri>http://localhost:8080/nuxeo</uri>
  <login>JohnDoe</login>
  <passwd>Doe</passwd>
  <input>../output/my-intermediate-file.xml</input>
  <path>/mydigitallibrary/books</path>
</destination>
```

**Example :** the intermediate file _my-intermediate-file.txt_
```xml
<nuxeomatic>
  <documents>
    <document name="unsolved-murders" type="Book" lifecycle="project">
      <acp/>
      <facets/>
      <attributes>
        <attr name="book:title">
          <value>Unsolved murders</value>
        </attr>
        <attr name="book:author">
          <value>Emily G. Thompson</value>
        </attr>
        <attr name="book:category">
          <value>Thriller</value>
        </attr>
        <attr name="book:creation">
          <value>05/02/2019</value>
        </attr>
      </attributes>
    </document>
    <document name="close-your-eyes" type="Book" lifecycle="project">
      <acp/>
      <facets/>
      <attributes>
        <attr name="book:title">
          <value>Close your eyes</value>
        </attr>
        <attr name="book:author">
          <value>Iris &amp; Roy Johansen</value>
        </attr>
        <attr name="book:category">
          <value>Thriller</value>
        </attr>
        <attr name="book:creation">
          <value>17/12/2012</value>
        </attr>
      </attributes>
    </document>
    <document name="knock-knock" type="Book" lifecycle="project">
      <acp/>
      <facets/>
      <attributes>
        <attr name="book:title">
          <value>Knock, knock</value>
        </attr>
        <attr name="book:author">
          <value>S.P. Miskowski</value>
        </attr>
        <attr name="book:category">
          <value>Horror</value>
        </attr>
        <attr name="book:creation">
          <value>01/01/1970</value>
        </attr>
      </attributes>
    </document>
  </documents>
</nuxeomatic>
```

---
## Nuxeo operation chain
To call chained content automation operations from a Nuxeo server instance.

```xml
<destination type="nuxeo-chain" name="{any string}">
  <uri>{a Nuxeo server access URL}</uri>
  <login>{the user name to connect with}</login>
  <passwd>{the user password to connect with}</passwd>
  <input>{an intermediate file containing the Nuxeo directives to execute}</input>
</destination>
```

**Example :** to create _book_ type documents within a Nuxeo server path _/mydigitallibrary/books_
```xml
<destination type="nuxeo-chain" name="My target Nuxeo instance">
  <uri>http://localhost:8080/nuxeo</uri>
  <login>JohnDoe</login>
  <passwd>Doe</passwd>
  <input>../output/my-intermediate-file.xml</input>
</destination>
```

**Example :** the intermediate file _my-intermediate-file.txt_ to copy and rename a Nuxeo document with identifier '1e80-0i855h9n-895y'
```xml
<?xml version="1.0" encoding="UTF-8"?>
<alambic>
  <chains>
    <chain id="copy-document" documentId="1e80-0i855h9n-895y">
      <!-- push the document with identifier ' 1e80-0i855h9n-895y' for later use -->
      <operation id="Context.SetInputAsVar">
        <param type="string" name="name">document</param>
      </operation>
      <!-- Will copy the document into folder with identifier '5hhp-e82s6rg0-2gm1' -->
      <operation id="Document.Copy">
        <param type="document" name="target">5hhp-e82s6rg0-2gm1</param>
        <header name="Nuxeo-Transaction-Timeout">600</header>
      </operation>
      <!-- pop up the document to update the title -->
      <operation id="Context.RestoreDocumentInput">
        <param type="string" name="name">document</param>
      </operation>
      <!-- Will rename the document -->
      <operation id="Document.SetProperty">
        <param type="string" name="xpath">dc:title</param>
        <param type="boolean" name="save">true</param>
        <param type="serializable" name="value">My new document title</param>
      </operation>
    </chain>
  </chains>
</alambic>
```

# Additional connectors capabilities
Some source connectors have additional capabilities :
1. a request can be dynamically run at runtime from a Freemarker directive,
2. the request result set can be paged and each page be processed by a concurrent thread.

## 1. Dynamic call
To configure a source connector so as to make a dynamic call at runtime via a Freemarker directive, add the following attribute : ***dynamic***
```xml
<resource type="{...}" name="{...}" dynamic="true">...</resource>
```

> **TO NOTICE :** 
> - as the resource will be dynamically requested at runtime via a Freemarker directive, its *query* XML definition element must be empty : ```<query/>```.
> - the following Freemarker directive ***Fn.query()*** can be used to request a dynamically resource :
> ```Freemarker
> <#-- query a BaseX resource via XPATH language to list the books belonging to the category 'Thriller' -->
> <#assign booksList=Fn.query(resources, "BaseX", "/books/book[@name='category' and value='Thriller']") />
>   <#if booksList?has_content>
>     <#-- convert the result set string representation into a XML wrapped object -->
>     <#assign booksNode=Fn.getNodeModelFromString("<root>" + booksList[0].item[0] + "</root>")/>
>     ...
> ```

## 2. Paged result set
To configure a source connector so as to parallelize the processing of each page of a result set, add the following attribute : ***page***
```xml
<resource type="{...}" name="{...}" dynamic="true" page="{the page size}">...</resource>
```
> **TO NOTICE :** 
> - the attribute ```dynamic="true"``` must be set to use the paging feature.
> - paging feature isn't supported by all features. The following matrix gets the resources capabilities.
> - when a job definition specifies multiple resources, only one of them can be paged.

## Capabilities matrix
|Connector|Do&nbsp;support&nbsp;dynamic&nbsp;call|Do&nbsp;support&nbsp;paged&nbsp;result&nbsp;set|Comments|
|---|---|---|---|
|**LDAP**|yes|yes|To make paged result set be supported, an Oracle LDAP (ODSEE) will require the definition of _Virtual List View_ objects (search & index) according to the query.|
|**Relational database**|yes|no||
|**Web service**|yes|no||
|**Nuxeo-NXQL**|yes|no||
|**File explorer**|yes|yes||
|**Grep**|yes|no||
|**CSV**|yes|no||
|**BaseX**|yes|yes||
|**Random&nbsp;generator&nbsp;-&nbsp;date**|yes|no||
|**Random&nbsp;generator&nbsp;-&nbsp;integer**|yes|no||
|**Random&nbsp;generator&nbsp;-&nbsp;fake&nbsp;user**|yes|no||
|**Random&nbsp;generator&nbsp;-&nbsp;UID**|yes|no||
|**Random&nbsp;generator&nbsp;-&nbsp;UUID**|yes|no||
