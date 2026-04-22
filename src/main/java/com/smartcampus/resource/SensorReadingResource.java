package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor not found: " + sensorId);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
        List<SensorReading> readingList = store.getReadings().getOrDefault(sensorId, List.of());
        return Response.ok(readingList).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor not found: " + sensorId);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is under maintenance and cannot accept readings.");
        }
        SensorReading newReading = new SensorReading(reading.getValue());
        sensor.setCurrentValue(newReading.getValue());
        store.getReadings().get(sensorId).add(newReading);
        return Response.status(Response.Status.CREATED).entity(newReading).build();
    }
}