/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.superbiz.moviefun.rest;

import org.superbiz.moviefun.Movie;
import org.superbiz.moviefun.MoviesBean;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

@Path("movies")
@Produces({"application/json"})
public class MoviesRest {

    @EJB
    private MoviesBean service;

    @GET
    @Path("{id}")
    public Response find(@PathParam("id") Long id) {
        final Movie movie = service.find(id);
        if (movie == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(movie).build();
    }

    @GET
    public Response getMovies(@QueryParam("first") Integer first, @QueryParam("max") Integer max,
                              @QueryParam("field") String field, @QueryParam("searchTerm") String searchTerm) {
        return Response.ok(service.getMovies(first, max, field, searchTerm)).build();
    }

    @POST
    @Consumes("application/json")
    public Response addMovie(Movie movie, @Context UriInfo uriInfo) {
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path(String.valueOf(movie.getId()));
        return Response.created(builder.build()).build();
    }

    @PUT
    @Path("{id}")
    @Consumes("application/json")
    public Response editMovie(Movie movie) {
        service.editMovie(movie);
        return Response.ok(movie).build();
    }

    @DELETE
    @Path("{id}")
    public void deleteMovie(@PathParam("id") long id) {
        service.deleteMovie(id);
    }

    @GET
    @Path("count")
    public Response count(@QueryParam("field") String field, @QueryParam("searchTerm") String searchTerm) {
        return Response.ok(String.valueOf(service.count(field, searchTerm))).build();
    }

}