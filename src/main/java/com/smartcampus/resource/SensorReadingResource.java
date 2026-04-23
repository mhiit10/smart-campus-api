package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
        if (!store.getSensors().containsKey(sensorId)) {
            return Response.status(404)
                .entity(Map.of("error", "Sensor not found: " + sensorId))
                .build();
        }
        List<SensorReading> list = store.getReadingsForSensor(sensorId);
        return Response.ok(list).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        var sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(404)
                .entity(Map.of("error", "Sensor not found: " + sensorId))
                .build();
        }
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }
        SensorReading r = new SensorReading(reading.getValue());
        store.getReadingsForSensor(sensorId).add(r);
        sensor.setCurrentValue(r.getValue());
        return Response.status(201).entity(r).build();
    }
}