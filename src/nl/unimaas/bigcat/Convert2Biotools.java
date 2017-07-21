package nl.unimaas.bigcat;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.SwaggerParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 * Convert OpenAPI json to bio.tools json 
 * 
 * @author jonathan
 *
 */
public class Convert2Biotools {	
	

	/**
	 * Constructor to run the conversion
	 * @param inputPath - the path of the file to convert
	 * @throws IOException
	 * @throws ParseException
	 */
	public Convert2Biotools(String inputPath) throws IOException, ParseException {		
		Swagger swagger = new SwaggerParser().read(inputPath);

		HashMap<String, String> formatMap = createFormatMap();
  
		JSONObject onto = readOntology(inputPath);
		
		JSONArray functions = new JSONArray();
		
		for ( Entry<String, Path> entry : swagger.getPaths().entrySet()){
			if (entry.getValue().getGet()!=null){
				Operation op = entry.getValue().getGet();
				
				//Add the handle field
				LinkedHashMap functionsMap = new LinkedHashMap();
				functionsMap.put("handle", entry.getKey());
				
				//Add the comment field
				try {
					if (op.getDescription()!=null & !op.getDescription().equals("")){
						//bio.tools only allows 1,000 characters for the comments
						if (op.getDescription().length()>1000){
							functionsMap.put("comment", op.getDescription().substring(0, 1000));						
						}
						else{
							functionsMap.put("comment", op.getDescription());
						}
					}
				} catch (NullPointerException e) {
					System.err.println("No comment found for: "+entry.getKey());
				}
				
				JSONArray operation = new JSONArray();
				
				//Add the operation field
				HashMap<String, String> operationMap = new HashMap<String,String>();
				operationMap.put("uri", "http://edamontology.org/operation_2422");
				operationMap.put("term","Data retrieval");				
				operation.add(operationMap);
				functionsMap.put("operation", operation);
				
				//Add the input field
				JSONArray input = new JSONArray();				
				for ( Parameter para : op.getParameters()){
					if (para.getRequired()){												
						String term = para.getName();
						Object obj = onto.get(term);
						//Retrieve the matching terms from the onto.json
						if ( obj instanceof JSONObject ){
							JSONObject jsonObj = (JSONObject) obj;
							HashMap<String, String> hashMap = new HashMap<String,String>();
							JSONObject data = new JSONObject();
							hashMap.put("uri", (String)jsonObj.get("uri"));
							hashMap.put("term", (String)jsonObj.get("term"));
							data.put("data",hashMap);	
							input.add(data);	
						}
						else{// add the generic term "Data" 
							HashMap<String, String> hashMap = new HashMap<String,String>();
							JSONObject data = new JSONObject();
							hashMap.put("term", "Data");
							data.put("data",hashMap);	
							input.add(data);							
						}
					}
				}			
				if (!input.isEmpty())functionsMap.put("input", input);
				
				//Add the output field
				JSONArray output = new JSONArray();				
				JSONObject dataOutput = new JSONObject();	
				JSONArray array = new JSONArray();	
				
				HashMap<String, String> hashMapOutput = new HashMap<String,String>();
				hashMapOutput.put("term", "Data");
				dataOutput.put("data",hashMapOutput);
							
				if	( op.getProduces()!=null)	{				
					for ( String prod : op.getProduces()){
						for(String term : prod.split(",")){
							if (!term.contains("vnd") & !term.contains("qs") & !term.contains("plain")) {
								HashMap<String, String> hashMap = new HashMap<String,String>();
								hashMap.put("uri", formatMap.get(term));					
								hashMap.put("term",formatter(term));
								array.add(hashMap);
							}
						}						
					}
					if (!array.isEmpty())
						dataOutput.put("format",array);					
				}
				output.add(dataOutput);
				functionsMap.put("output", output);
				
				functions.add(functionsMap);
			}
		}
		JSONObject jsonOutput = new JSONObject();
		jsonOutput.put("function", functions);
		
		String outputPath = inputPath.replace(".json", "_biotools.json");
		outputPath = outputPath.replace("input", "output");
		
		try (FileWriter file = new FileWriter(outputPath)) {
			file.write(jsonOutput.toJSONString());
		}
	}
	
	/**
	 * Read and parse the ontology file which contains the matching terms
	 * @param inputPath -the path of the file to convert
	 * @return JSONObject - the ontology json object which contains the matching term
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 */
	public JSONObject readOntology(String inputPath) throws FileNotFoundException, IOException, ParseException{
		String onto = inputPath.replace(".json", "_onto.json");
		onto = onto.replace("input", "ontology");
		JSONParser parser = new JSONParser();		
		Object parse = parser.parse(new FileReader(onto));
		JSONObject jsonObject = (JSONObject) parse;
		
		return jsonObject;
	}
	
	
	/**
	 * Replace the output extension term to a bio.tools output term 
	 * @param term
	 * @return
	 */
	public String formatter(String term){
		term = term.replaceAll("x-gff", "GFF");
		term = term.replaceAll("x-fasta", "fasta");
		
		term = term.replaceAll("ld\\+json", "JSON-LD");
		term = term.replaceAll("application/ld+json", "JSON-LD");
		term = term.replaceAll("application/", "");
		term = term.replaceAll("text/", "");
		
		return term.toUpperCase();
	}
	
	
	/**
	 * Create a map to match the output text to an ontology URI 
	 * @return formatMap
	 */
	public HashMap<String,String> createFormatMap(){
		HashMap<String,String> formatMap = new HashMap<String,String>();
		
		formatMap.put("application/json","http://edamontology.org/format_3464");
		formatMap.put("text/xml","http://edamontology.org/format_2332");
		
		formatMap.put("application/xml","http://edamontology.org/format_2332");
		formatMap.put("application/ld+json","http://edamontology.org/format_3749");
		
		formatMap.put("text/html","http://edamontology.org/format_2331");
		formatMap.put("text/x-gff","http://edamontology.org/format_2305");
		formatMap.put("text/plain","http://edamontology.org/format_1964");
		formatMap.put("text/tab-separated-values","http://edamontology.org/format_3475");
		formatMap.put("text/csv","http://edamontology.org/format_3752");
		formatMap.put("text/x-fasta","http://edamontology.org/format_1929");
		
		formatMap.put("Format","http://edamontology.org/format_3475");
		return formatMap;
	}
	
	
	public static void main(String[] args) throws IOException, ParseException {
		File dir = new File("src/nl/unimaas/bigcat/resources/input");
		
		FilenameFilter textFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				if (lowercaseName.endsWith(".json")) {
					return true;
				} else {
					return false;
				}
			}
		};
		
		File[] listFiles = dir.listFiles(textFilter);			
		for(File f : listFiles){
			System.out.println("Parsing now: "+f.getName());
			new Convert2Biotools(f.getAbsolutePath());	
		}
	}
}
