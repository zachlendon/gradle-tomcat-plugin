/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.plugins.tomcat

import org.apache.catalina.connector.Connector
import org.apache.catalina.startup.Embedded
import org.gradle.api.GradleException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.plugins.tomcat.internal.ShutdownMonitor
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.catalina.*

/**
 * Base class for all tasks which deploy a web application to an embedded Tomcat web container.
 *
 * @author Benjamin Muschko
 */
abstract class AbstractTomcatRunTask extends ConventionTask {
    private static Logger logger = LoggerFactory.getLogger(AbstractTomcatRunTask.class);
    protected boolean reloadable
    private String contextPath
    private Integer httpPort
    private Integer stopPort
    private String stopKey

    private File webDefaultXml
    private Embedded server
    private Context context
    private Realm realm

    public abstract void validateConfiguration()
    public abstract void configureWebApplication()

    @TaskAction
    protected void start() {
        startTomcat()
    }

    public void startTomcat() {
        logger.info "Configuring Tomcat for ${getProject()}"
        validateConfiguration()
        startTomcatInternal()
    }

    void startTomcatInternal() {
        try {
            logger.debug "Starting Tomcat Server ..."

            setServer(createServer())
            getServer().setCatalinaHome(getTemporaryDir().getAbsolutePath())
            getServer().setRealm(realm)

            configureWebApplication()
          
            // Create host
            Host localHost = getServer().createHost("localHost", new File(".").getAbsolutePath())
            localHost.addChild(context)

            // Create engine
            Engine engine = getServer().createEngine();
            engine.setName("localEngine");
            engine.addChild(localHost);
            engine.setDefaultHost(localHost.getName());
            getServer().addEngine(engine);

            // Create HTTP connector
            Connector httpConnector = server.createConnector((InetAddress) null, getHttpPort(), false)
            getServer().addConnector(httpConnector)

            // Start server
            getServer().start()

            logger.info "Started Tomcat Server"

            new ShutdownMonitor(getStopPort()).start()
        }
        catch(Exception e) {
            throw new GradleException("An error occurred starting the Tomcat server.", e)
        }
        finally {
            logger.info "Tomcat server exiting."
        }
    }

    void configureDefaultServlet() {
        Wrapper defaultServlet = getContext().createWrapper()

        defaultServlet.with {
            setName "default"
            setServletClass "org.apache.catalina.servlets.DefaultServlet"
            addInitParameter "debug", "0"
            addInitParameter "listings", "false"
            setLoadOnStartup 1
        }

		getContext().addChild defaultServlet
		getContext().addServletMapping "/", "default"
    }

    void configureJspServlet() {
        Wrapper jspServlet = getContext().createWrapper()

        jspServlet.with {
            setName "jsp"
            setServletClass "org.apache.jasper.servlet.JspServlet"
            addInitParameter "fork", "false"
            addInitParameter "xpoweredBy", "false"
            setLoadOnStartup 3
        }

		getContext().addChild jspServlet
		getContext().addServletMapping "*.jsp", "jsp"
		getContext().addServletMapping "*.jspx", "jsp"
    }

    Embedded createServer() {
        new Embedded()
    }

    public Embedded getServer() {
        server
    }

    public void setServer(Embedded server) {
        this.server = server
    }

    public Context getContext() {
        context
    }

    public void setContext(Context context) {
        this.context = context
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public Integer getStopPort() {
        return stopPort;
    }

    public void setStopPort(Integer stopPort) {
        this.stopPort = stopPort;
    }

    public String getStopKey() {
        return stopKey;
    }

    public void setStopKey(String stopKey) {
        this.stopKey = stopKey;
    }

    @InputFile
    @Optional
    public File getWebDefaultXml() {
        return webDefaultXml;
    }

    public void setWebDefaultXml(File webDefaultXml) {
        this.webDefaultXml = webDefaultXml;
    }
}