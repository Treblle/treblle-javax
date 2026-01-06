package com.treblle.javax.infrastructure;

import com.treblle.common.infrastructure.RequestWrapper;

import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;

public class ContainerRequestContextWrapper implements RequestWrapper {

    private final ContainerRequestContext containerRequestContext;
    private final ResourceInfo resourceInfo;

    public ContainerRequestContextWrapper(ContainerRequestContext containerRequestContext, ResourceInfo resourceInfo) {
        this.containerRequestContext = containerRequestContext;
        this.resourceInfo = resourceInfo;
    }

    @Override
    public String getProtocol() {
        return containerRequestContext.getUriInfo().getRequestUri().getScheme();
    }

    @Override
    public String getMethod() {
        return containerRequestContext.getMethod();
    }

    @Override
    public String getUrl() {
        return containerRequestContext.getUriInfo().getRequestUri().toString();
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        MultivaluedMap<String, String> headers = containerRequestContext.getHeaders();
        return Collections.enumeration(headers.keySet());    }

    @Override
    public String getHeader(String header) {
        return containerRequestContext.getHeaderString(header);
    }

    @Override
    public String getRemoteAddr() {
        return containerRequestContext.getHeaders().getFirst("X-Forwarded-For");
    }

    @Override
    public String getServerAddr() {
        return null;
    }

    @Override
    public Map<String, String> getQueryParams() {
        return containerRequestContext.getUriInfo().getQueryParameters().entrySet()
                .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> String.join(",", e.getValue())));
    }

    @Override
    public String getRoutePath() {
        if (resourceInfo == null) {
            return null;
        }

        Class<?> resourceClass = resourceInfo.getResourceClass();
        Method resourceMethod = resourceInfo.getResourceMethod();

        if (resourceClass == null || resourceMethod == null) {
            return null;
        }

        StringBuilder routePath = new StringBuilder();

        // Get class-level @Path
        Path classPath = resourceClass.getAnnotation(Path.class);
        if (classPath != null) {
            String path = classPath.value();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            routePath.append(path);
        }

        // Get method-level @Path
        Path methodPath = resourceMethod.getAnnotation(Path.class);
        if (methodPath != null) {
            String path = methodPath.value();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (routePath.length() > 0 && !routePath.toString().endsWith("/") && !path.isEmpty()) {
                routePath.append("/");
            }
            routePath.append(path);
        }

        String result = routePath.toString();
        // Remove trailing slash if present
        if (result.endsWith("/") && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }

        return result.isEmpty() ? null : result;
    }

    @Override
    public String getServerSoftware() {
        // Try to detect from system properties or common JAX-RS containers
        String serverInfo = System.getProperty("jboss.server.name");
        if (serverInfo != null) {
            String version = System.getProperty("jboss.server.version");
            return serverInfo + (version != null ? "/" + version : "");
        }

        // Check for Glassfish/Payara
        serverInfo = System.getProperty("glassfish.version");
        if (serverInfo != null) {
            return "GlassFish/" + serverInfo;
        }

        // Check for WebLogic
        serverInfo = System.getProperty("weblogic.Name");
        if (serverInfo != null) {
            String version = System.getProperty("weblogic.version");
            return "WebLogic" + (version != null ? "/" + version : "");
        }

        // Generic fallback
        return "JAX-RS Container";
    }
}
