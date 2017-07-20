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


public class Convert2Biotools {	
	

	public Convert2Biotools(String inputFile) throws IOException, ParseException {		
		Swagger swagger = new SwaggerParser().read(inputFile);

		HashMap<String, String> formatMap = createFormatMap();
  
		JSONObject onto = readOntology(inputFile);
		
		JSONObject jsonOutput = new JSONObject();
		
		JSONArray functions = new JSONArray();
		for ( Entry<String, Path> entry : swagger.getPaths().entrySet()){
			if (entry.getValue().getGet()!=null){
				Operation op = entry.getValue().getGet();
				
				LinkedHashMap linkedMap = new LinkedHashMap();
				linkedMap.put("handle", entry.getKey());
				
				if (op.getDescription()!=null & !op.getDescription().equals("")){
					if (op.getDescription().length()>1000){
						linkedMap.put("comment", op.getDescription().substring(0, 1000));						
					}
					else{
						linkedMap.put("comment", op.getDescription());
					}
				}
				
				JSONArray operation = new JSONArray();
				

				HashMap<String, String> operationMap = new HashMap<String,String>();
				operationMap.put("uri", "http://edamontology.org/operation_2422");
				operationMap.put("term","Data retrieval");
				
				operation.add(operationMap);
				linkedMap.put("operation", operation);
								
				JSONArray input = new JSONArray();
				
				int required = 0;
				for ( Parameter para : op.getParameters()){
					if (para.getRequired()){												
						String term = para.getName();
						Object obj = onto.get(term);
						if ( obj instanceof JSONObject ){
							JSONObject jsonObj = (JSONObject) obj;
							HashMap<String, String> hashMap = new HashMap<String,String>();
							JSONObject data = new JSONObject();
							hashMap.put("uri", (String)jsonObj.get("uri"));
							hashMap.put("term", (String)jsonObj.get("term"));
							data.put("data",hashMap);	
							input.add(data);	
						}
						else{
							HashMap<String, String> hashMap = new HashMap<String,String>();
							JSONObject data = new JSONObject();
							hashMap.put("term", "Data");
							data.put("data",hashMap);	
							input.add(data);							
						}
						required ++;
					}
				}
				
				if (required==0){
					HashMap<String, String> hashMap = new HashMap<String,String>();
					JSONObject data = new JSONObject();
					hashMap.put("term", "Data");
					data.put("data",hashMap);	
					input.add(data);	
				}
				
				linkedMap.put("input", input);
				
				JSONArray output = new JSONArray();
				
				JSONObject dataOutput = new JSONObject();	
				JSONArray array = new JSONArray();	
				JSONArray topArray = new JSONArray();	
				
				HashMap<String, String> hashMapOutput = new HashMap<String,String>();
				hashMapOutput.put("term", "Data");
				dataOutput.put("data",hashMapOutput);

				
				JSONObject format = new JSONObject();
							
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
				linkedMap.put("output", output);
				functions.add(linkedMap);
			}
		}
		jsonOutput.put("function", functions);
		
		String outputFile = inputFile.replace(".json", "_biotools.json");
		outputFile = outputFile.replace("input", "output");
		
		try (FileWriter file = new FileWriter(outputFile)) {
			file.write(jsonOutput.toJSONString());
		}
	}
	
	public static JSONObject readOntology(String inputFile) throws FileNotFoundException, IOException, ParseException{
		String onto = inputFile.replace(".json", "_onto.json");
		onto = onto.replace("input", "ontology");
		JSONParser parser = new JSONParser();		
		Object parse = parser.parse(new FileReader(onto));
		JSONObject jsonObject = (JSONObject) parse;
		
		return jsonObject;
	}
	
	
	public String formatter(String term){
		term = term.replaceAll("x-gff", "GFF");
		term = term.replaceAll("x-fasta", "fasta");
		
		term = term.replaceAll("ld\\+json", "JSON-LD");
		term = term.replaceAll("application/ld+json", "JSON-LD");
		term = term.replaceAll("application/", "");
		term = term.replaceAll("text/", "");
		
		return term.toUpperCase();
	}
	
	
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
		File dir = new File("ressource/input/");
		
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
			new Convert2Biotools(f.getAbsolutePath());	
		}
	}

}
