# swagger2BioTools
Tool to convert Swagger configuration files to Bio.Tools input XML.

#### Steps:

###### 1. Build the project
 
```
mvn clean install
```

###### 2. Run this command to create the ontologies templates
 
```
mvn exec:java -Dexec.mainClass="nl.unimaas.bigcat.OntologiesMapTemplateBuilder"
```

The ontologies files are in resources/ontology. 

###### 3. Annotate the terms when it's possible

E.g:
	
```
"symbol": "null"
```
annotated to 
```
"symbol": {
		"term": "Gene symbol",
		"uri":	"http://edamontology.org/data_1026"
}
```

###### 4.Run this command to convert
```
mvn exec:java -Dexec.mainClass="nl.unimaas.bigcat.Convert2Biotools"
```
It will convert all .json files present in resources/input.
The output files are in resources/output

The proposal can be found at https://github.com/bio-tools/Studentships/blob/master/openAPI.pdf
