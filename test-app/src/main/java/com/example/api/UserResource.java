package com.example.api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private static final Map<Integer, User> users = new HashMap<>();

    static {
        users.put(1, new User(1, "John Doe", "john@example.com"));
        users.put(2, new User(2, "Jane Smith", "jane@example.com"));
    }

    @GET
    public Response getAllUsers() {
        return Response.ok(new ArrayList<>(users.values())).build();
    }

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") int id) {
        User user = users.get(id);
        if (user == null) {
            return Response.status(404)
                .entity(Map.of("error", "User not found"))
                .build();
        }
        return Response.ok(user).build();
    }

    @POST
    public Response createUser(User user) {
        if (user.getName() == null || user.getEmail() == null) {
            return Response.status(400)
                .entity(Map.of("error", "Name and email are required"))
                .build();
        }
        int id = users.size() + 1;
        user.setId(id);
        users.put(id, user);
        return Response.status(201).entity(user).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateUser(@PathParam("id") int id, User user) {
        if (!users.containsKey(id)) {
            return Response.status(404)
                .entity(Map.of("error", "User not found"))
                .build();
        }
        user.setId(id);
        users.put(id, user);
        return Response.ok(user).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteUser(@PathParam("id") int id) {
        User removed = users.remove(id);
        if (removed == null) {
            return Response.status(404)
                .entity(Map.of("error", "User not found"))
                .build();
        }
        return Response.ok(Map.of("message", "User deleted successfully")).build();
    }

    public static class User {
        private Integer id;
        private String name;
        private String email;

        public User() {}

        public User(Integer id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
