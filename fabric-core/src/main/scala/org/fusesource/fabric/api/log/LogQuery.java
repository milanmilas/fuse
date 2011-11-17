/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api.log;

import org.apache.karaf.shell.log.LruList;
import org.apache.karaf.shell.log.VmLogAppender;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.fusesource.fabric.internal.Predicate;
import org.fusesource.fabric.internal.log.Logs;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class LogQuery implements LogQueryMBean {
    private transient Logger logger = LoggerFactory.getLogger(LogQuery.class);

    private BundleContext bundleContext;
    private VmLogAppender appender;
    private ObjectName mbeanName;
    private ObjectMapper mapper = new ObjectMapper();
    private ServiceTracker serviceTracker;

    public LogQuery() {
        mapper.getSerializationConfig().withSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
    }

    public void init() throws Exception {
        if (bundleContext == null) {
            throw new IllegalArgumentException("No bundleContext injected!");
        }
        ServiceTrackerCustomizer customizer = null;
        serviceTracker = new ServiceTracker(bundleContext, "org.ops4j.pax.logging.spi.PaxAppender", customizer);
        serviceTracker.open();
    }

    public void destroy() throws Exception {
        if (serviceTracker != null) {
            serviceTracker.close();
            serviceTracker = null;
        }
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ObjectName getMbeanName() throws MalformedObjectNameException {
        if (mbeanName == null) {
            mbeanName = new ObjectName("org.fusesource.fabric:type=LogQuery");
        }
        return mbeanName;
    }

    public void setMbeanName(ObjectName mbeanName) {
        this.mbeanName = mbeanName;
    }

    public void registerMBeanServer(MBeanServer mbeanServer) {
        try {
            ObjectName name = getMbeanName();
            ObjectInstance objectInstance = mbeanServer.registerMBean(this, name);
        } catch (Exception e) {
            logger.warn("An error occured during mbean server registration: " + e, e);
        }
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            try {
                mbeanServer.unregisterMBean(getMbeanName());
            } catch (Exception e) {
                logger.warn("An error occured during mbean server registration: " + e, e);
            }
        }
    }

    @Override
    public String filterLogEvents(String jsonFilter) throws IOException {
        LogFilter filter = jsonToLogFilter(jsonFilter);
        List<LogEvent> events = getLogEventList(filter);
        return toJSON(events);
    }

    @Override
    public String getLogEvents(int count) throws IOException {
        List<LogEvent> events = getLogEventList(count, null);
        return toJSON(events);
    }

    protected String toJSON(List<LogEvent> answer) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, answer);
            return writer.toString();
        } catch (IOException e) {
            logger.warn("Failed to marshal the events: " + e, e);
            throw new IOException(e.getMessage());
        }
    }

    protected LogFilter jsonToLogFilter(String json) throws IOException {
        if (json == null) {
            return null;
        }
        json = json.trim();
        if (json.length() == 0 || json.equals("{}")) {
            return null;
        }
        return mapper.reader(LogFilter.class).readValue(json);
    }


    public  List<LogEvent> getLogEventList(LogFilter filter) {
        Predicate<PaxLoggingEvent> predicate = Logs.createPredicate(filter);
        int count = -1;
        if (filter != null) {
            count = filter.getCount();
        }
        return getLogEventList(count, predicate);
    }

    public List<LogEvent> getLogEventList(int count, Predicate<PaxLoggingEvent> predicate) {
        List<LogEvent> answer = new ArrayList<LogEvent>();
        VmLogAppender a = getAppender();
        if (a != null) {
            LruList events = a.getEvents();
            Iterable<PaxLoggingEvent> iterable;
            if (count > 0) {
                iterable = events.getElements(count);
            } else {
                iterable = events.getElements();
            }
            for (PaxLoggingEvent event : iterable) {
                if (predicate == null || predicate.matches(event)) {
                    answer.add(Logs.newInstance(event));
                }
            }
        } else {
            logger.warn("No VmLogAppender available!");
        }
        return answer;
    }

    public VmLogAppender getAppender() {
        if (appender == null && serviceTracker != null) {
            Object[] services = serviceTracker.getServices();
            if (services != null) {
                for (Object service : services) {
                    if (service instanceof VmLogAppender) {
                        return (VmLogAppender) service;
                    }
                }
            }
        }
        return appender;
    }

    public void setAppender(VmLogAppender appender) {
        this.appender = appender;
    }
}