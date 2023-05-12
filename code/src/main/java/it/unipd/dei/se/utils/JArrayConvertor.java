package it.unipd.dei.se.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;

/**
 * A class to convert a JsonArray to a different type of array.
 * @author CLOSE GROUP
 * @version 1.0
 */
public class JArrayConvertor {

    /** Default constructor for the class
     */
    public JArrayConvertor(){
        super();
    }

    /**
     * Convert a JsonArray to a float array
     *
     * @param jsonArray the JsonArray to convert
     * @return the float array converted
     * @throws JsonIOException if the JsonArray is not valid
     */
    public static float[] toFloatArray(JsonArray jsonArray) throws JsonIOException {
        float[] fData = new float[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            try {
                fData[i] = jsonArray.get(i).getAsFloat();
            } catch (JsonIOException e) {
                throw new JsonIOException("The JsonArray is not valid");
            }
        }
        return fData;
    }
}
