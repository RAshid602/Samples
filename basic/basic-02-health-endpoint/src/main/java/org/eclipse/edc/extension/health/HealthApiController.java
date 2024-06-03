/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.extension.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/")
public class HealthApiController {

    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final HealthService healthService;

    public HealthApiController(Monitor monitor) {
        this.monitor = monitor;
        this.objectMapper = new ObjectMapper();
        this.healthService = new HealthService();
    }

    @GET
    @Path("health")
    public String checkHealth() {
        monitor.info("Received a health request");
        return "{\"response\":\"I'm alive!\"}";
    }

    @POST
    @Path("users")
    public Response addUsers(String json) {
        try {
            // Deserialize JSON to Users object
            Users users = objectMapper.readValue(json, Users.class);
            // Save user to database
            healthService.saveUserToDatabase(users);
            monitor.info(String.format("%s :: User Added", users.getName()));
            return Response.ok(json).build();
        } catch (IOException e) {
            monitor.severe("Error processing JSON", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invalid JSON format\"}")
                    .build();
        } catch (SQLException e) {
            monitor.severe("Error saving user to database", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Database error\"}")
                    .build();
        }
    }

    @GET
    @Path("users")
    public Response getUsers() {
        try {
            // Retrieve users from database
            List<Users> usersList = healthService.getUsersFromDatabase();
            // Serialize users list to JSON
            String jsonResult = objectMapper.writeValueAsString(usersList);
            return Response.ok(jsonResult).build();
        } catch (SQLException e) {
            monitor.severe("Error retrieving users from database", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Database error\"}")
                    .build();
        } catch (IOException e) {
            monitor.severe("Error converting users to JSON", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"JSON conversion error\"}")
                    .build();
        }
    }
}
