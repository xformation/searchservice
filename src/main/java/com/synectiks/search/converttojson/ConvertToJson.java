package com.synectiks.search.converttojson;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.io.PatternFilenameFilter;

public class ConvertToJson {

	private static final String INPUT_DIR = "C:\\Users\\admin\\Desktop\\cms_test_data";
	
	public static void main(String a[]) throws IOException {
		File[] ary = listFiles();
		createIndex(ary);
		System.out.println("COMPLETED ");
	}
	public static void createIndex(File [] files) {
		for(File file: files) {
			System.out.println(file.getName());
			// call search serive api to create index
		}
	}
	
	public static File[] listFiles() throws IOException {
		File inputDir = new File(INPUT_DIR);
		File files[] = inputDir.listFiles(new PatternFilenameFilter(".*\\.csv"));
		for(File file: files) {
			System.out.println(file.getName());
			readObjectsFromCsv(file);
			System.out.println();
		}
		
		return files;
	}
	
	public static String readObjectsFromCsv(File file) throws IOException {
		CsvSchema bootstrap = CsvSchema.builder().setUseHeader(true).setColumnSeparator(';').build();
		
		CsvMapper csvMapper = new CsvMapper();
		List<Object> readAll = csvMapper.readerFor(Map.class).with(bootstrap).readValues(file).readAll();
		ObjectMapper mapper = new ObjectMapper();
		 
        // Write JSON formated data to output.json file
//        mapper.writerWithDefaultPrettyPrinter().writeValue(output, readAll);
 
        // Write JSON formated data to stdout
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(readAll);
        System.out.println(json);
        return json;
    }

//    public static void writeAsJson(List<Map<?, ?>> data, File file) throws IOException {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.writeValue(file, data);
//    }
    

}
