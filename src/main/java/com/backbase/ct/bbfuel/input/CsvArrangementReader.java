package com.backbase.ct.bbfuel.input;

import static com.backbase.ct.bbfuel.data.CommonConstants.PROPERTY_CSV_FILE_LOCATION;

import com.backbase.integration.arrangement.rest.spec.v2.arrangements.ArrangementsPostRequestBody;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.util.List;

public class CsvArrangementReader extends BaseReader {

    public List<ArrangementsPostRequestBody>  retrieveArrangementsPostRequestBodiesFromFile() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(this.globalProperties.getString(PROPERTY_CSV_FILE_LOCATION)).getFile());

        return readFileAsArrangementsPostRequestBodies(file);
    }

    private static List<ArrangementsPostRequestBody> readFileAsArrangementsPostRequestBodies(File csvFile) throws Exception {
        CsvSchema schema = CsvSchema.emptySchema().withHeader(); // use first row as header

        MappingIterator<ArrangementsPostRequestBody> arrangementsPostRequestBodyMappingIterator = new CsvMapper()
            .readerFor(ArrangementsPostRequestBody.class).with(schema).readValues(csvFile);

        return arrangementsPostRequestBodyMappingIterator.readAll();
    }
}
