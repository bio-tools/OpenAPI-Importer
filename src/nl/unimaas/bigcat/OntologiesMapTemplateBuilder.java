package nl.unimaas.bigcat;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.SwaggerParser;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONObject;

public class OntologiesMapTemplateBuilder {

	
	public OntologiesMapTemplateBuilder (String source) throws IOException{
		Swagger swagger = new SwaggerParser().read(source);
		Set<String> set = new HashSet<String>();

		for ( Entry<String, Path> entry : swagger.getPaths().entrySet()){
			if (entry.getValue().getGet()!=null){
				Operation op = entry.getValue().getGet();
				for ( Parameter para : op.getParameters()){
					if (para.getRequired()){												
						String name = para.getName();
						set.add(name);
					}
				}
			}
		}

		JSONObject output = new JSONObject();
		for (String name : set){
			output.put(name, "null");
		}

		String ontology = source.replace(".json", "_onto.json");
		ontology = ontology.replace("input", "ontology"); 
		try (FileWriter file = new FileWriter(ontology)) {
			file.write(output.toJSONString());
		}
	
	}
	public static void main(String[] args) throws IOException {
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
			System.out.println("Building template: "+f.getName());
			new OntologiesMapTemplateBuilder(f.getAbsolutePath());
		}
	}
}