/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server;

import com.thoughtworks.go.util.OperatingSystem;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.Resource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

public class Jetty6AssetsContextHandlerTest {
    @Test
    public void shouldSetHeadersAndBaseDirectory() throws IOException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        WebAppContext webAppContext = mock(WebAppContext.class);
        when(webAppContext.getInitParameter("rails.root")).thenReturn("rails.root");
        when(webAppContext.getWebInf()).thenReturn(Resource.newResource("WEB-INF"));
        Jetty6AssetsContextHandler handler = new Jetty6AssetsContextHandler(systemEnvironment);
        handler.init(webAppContext);
        assertThat(handler.getHandler() instanceof ResourceHandler, is(true));
        ResourceHandler resourceHandler = (ResourceHandler) handler.getHandler();
        assertThat(resourceHandler.getCacheControl(), is("max-age=31536000,public"));
        assertThat(resourceHandler.getResourceBase(), isSameFileAs(new File("WEB-INF/rails.root/public/assets").toURI().toString()));
    }

    @Test
    public void shouldHandleIfTargetMatchesContextPath() throws IOException, ServletException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        WebAppContext webAppContext = mock(WebAppContext.class);
        when(webAppContext.getInitParameter("rails.root")).thenReturn("rails.root");
        when(webAppContext.getWebInf()).thenReturn(Resource.newResource("WEB-INF"));
        Jetty6AssetsContextHandler handler = spy(new Jetty6AssetsContextHandler(systemEnvironment));
        handler.init(webAppContext);
        Request request = mock(Request.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.isHandled()).thenReturn(false);
        handler.handle("/go/assets/junk", request, response, 1);
        verify(handler).superDotHandle("/go/assets/junk", request, response, 1);
    }

    @Test
    public void shouldNotHandleOnlyIfTargetDoesNotMatcheContextPath() throws IOException, ServletException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        WebAppContext webAppContext = mock(WebAppContext.class);
        when(webAppContext.getInitParameter("rails.root")).thenReturn("rails.root");
        when(webAppContext.getWebInf()).thenReturn(Resource.newResource("WEB-INF"));
        Jetty6AssetsContextHandler handler = spy(new Jetty6AssetsContextHandler(systemEnvironment));
        handler.init(webAppContext);
        Request request = mock(Request.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.isHandled()).thenReturn(false);
        handler.handle("/junk", request, response, 1);
        verify(handler, never()).superDotHandle("/junk", request, response, 1);
    }

    @Test
    public void shouldNotHandleForRails4DevelopmentMode() throws IOException, ServletException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.useCompressedJs()).thenReturn(false);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        WebAppContext webAppContext = mock(WebAppContext.class);
        when(webAppContext.getInitParameter("rails.root")).thenReturn("rails.root");
        when(webAppContext.getWebInf()).thenReturn(Resource.newResource("WEB-INF"));
        Jetty6AssetsContextHandler handler = spy(new Jetty6AssetsContextHandler(systemEnvironment));
        handler.init(webAppContext);
        Request request = mock(Request.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.isHandled()).thenReturn(false);
        handler.handle("/go/assets/junk", request, response, 1);
        verify(handler, never()).superDotHandle("/go/assets/junk", request, response, 1);
    }

    private Matcher<? super String> isSameFileAs(final String expected) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object o) {
                String actualFile = (String) o;

                if (OperatingSystem.WINDOWS.equals(new SystemEnvironment().getCurrentOperatingSystem())) {
                    return expected.equalsIgnoreCase(actualFile);
                }
                return expected.equals(actualFile);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("     " + expected);
            }
        };
    }
}
