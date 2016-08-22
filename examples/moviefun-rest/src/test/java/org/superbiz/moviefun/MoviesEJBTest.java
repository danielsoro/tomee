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
package org.superbiz.moviefun;

import org.apache.cxf.jaxrs.client.WebClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.superbiz.moviefun.rest.MoviesRest;

import javax.ejb.EJB;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class MoviesEJBTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(Movie.class, MoviesBean.class, MoviesRest.class)
                .addAsResource(new ClassLoaderAsset("META-INF/ejb-jar.xml"), "META-INF/ejb-jar.xml")
                .addAsResource(new ClassLoaderAsset("META-INF/persistence.xml"), "META-INF/persistence.xml");
    }

    @ArquillianResource
    private URL webappUrl;

    @EJB
    private MoviesBean movies;

    @Before
    @After
    public void clean() {
        movies.clean();
    }

    @Test
    public void shouldBeAbleToAddAMovie() throws Exception {
        assertNotNull("Verify that the ejb was injected", movies);

        final Movie movie = new Movie();
        movie.setDirector("Michael Bay");
        movie.setGenre("Action");
        movie.setRating(9);
        movie.setTitle("Bad Boys");
        movie.setYear(1995);
        movies.addMovie(movie);

        assertEquals(1, movies.count("title", "a"));
        final List<Movie> moviesFound = movies.getMovies(0, 100, "title", "Bad Boys");
        assertEquals(1, moviesFound.size());
        assertEquals("Michael Bay", moviesFound.get(0).getDirector());
        assertEquals("Action", moviesFound.get(0).getGenre());
        assertEquals(9, moviesFound.get(0).getRating());
        assertEquals("Bad Boys", moviesFound.get(0).getTitle());
        assertEquals(1995, moviesFound.get(0).getYear());
    }

    @Test
    public void postAndGet() throws Exception {
        // POST
        {
            final Movie movie = new Movie();
            movie.setDirector("Steven Spielberg");
            movie.setGenre("Science fiction");
            movie.setRating(10);
            movie.setTitle("E.T. the Extra-Terrestrial");
            movie.setYear(1982);
            final WebClient webClient = WebClient.create(webappUrl.toURI());
            final Response response = webClient.path("movies")
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON)
                    .post(movie);

            assertEquals(201, response.getStatus());
        }

        // GET
        {
            final WebClient webClient = WebClient.create(webappUrl.toURI());
            final Response response = webClient.path("movies").get();

            assertEquals(200, response.getStatus());
            assertNotNull(response.getEntity());
        }

        // GET
        {
            final WebClient webClient = WebClient.create(webappUrl.toURI());
            final Response response = webClient.path("movies/count").get();

            assertEquals(200, response.getStatus());
            assertNotNull(response.getEntity());
            assertEquals(String.valueOf(0), slurp((InputStream) response.getEntity()));
        }
    }

    public static String slurp(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }
        out.flush();
        return new String(out.toByteArray());
    }

}
