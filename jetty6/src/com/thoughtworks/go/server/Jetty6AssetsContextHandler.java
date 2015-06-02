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

import com.thoughtworks.go.util.SystemEnvironment;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.webapp.WebAppContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Jetty6AssetsContextHandler extends ContextHandler {
    private SystemEnvironment systemEnvironment;

    public Jetty6AssetsContextHandler(SystemEnvironment systemEnvironment) throws IOException {
        super(systemEnvironment.getWebappContextPath() + "/assets");
        this.systemEnvironment = systemEnvironment;
    }

    public void init(WebAppContext webAppContext) throws IOException {
        ResourceHandler resourceHandler = new ResourceHandler();
        String railsRootDirName = webAppContext.getInitParameter("rails.root").replaceAll("/WEB-INF/", "");
        String assetsDir = webAppContext.getWebInf().addPath(String.format("%s/public/assets/", railsRootDirName)).getName();
        resourceHandler.setResourceBase(assetsDir);
        resourceHandler.setCacheControl("max-age=31536000,public");
        this.setHandler(resourceHandler);
    }

    public void handle(String target, javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, int dispatch) throws IOException, javax.servlet.ServletException {
        if (shouldNotHandle()) return;
        Request base_request = (request instanceof Request) ? (Request) request : HttpConnection.getCurrentConnection().getRequest();
        if (target.startsWith(this.getContextPath()) && !base_request.isHandled()) {
            superDotHandle(target, request, response, dispatch);
        }
    }

    void superDotHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        super.handle(target, request, response, dispatch);
    }

    private boolean shouldNotHandle() {
        return !systemEnvironment.useCompressedJs();
    }
}
