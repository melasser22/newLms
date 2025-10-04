package org.springframework.mock.web;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight replacement for Spring's {@code MockHttpServletRequest} used in unit tests.
 * It only implements the small subset of behaviour required by {@code DefaultTenantResolverTest}.
 */
public class MockHttpServletRequest implements HttpServletRequest {

    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private String method = "GET";
    private String serverName = "localhost";
    private String requestUri = "/";

    @Override
    public String getAuthType() {
        return null;
    }

    public void addHeader(String name, String value) {
        Objects.requireNonNull(name, "name");
        headers.computeIfAbsent(name.toLowerCase(Locale.ROOT), key -> new ArrayList<>()).add(value);
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setRequestURI(String requestUri) {
        this.requestUri = requestUri;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String getHeader(String name) {
        if (name == null) {
            return null;
        }
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (name == null) {
            return Collections.emptyEnumeration();
        }
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return Collections.enumeration(values == null ? List.of() : values);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public long getDateHeader(String name) {
        String value = getHeader(name);
        if (value == null) {
            return -1L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Cannot parse header value to date: " + value, ex);
        }
    }

    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        if (value == null) {
            return -1;
        }
        return Integer.parseInt(value);
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return requestUri;
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer("http://" + getServerName() + requestUri);
    }

    @Override
    public String getServletPath() {
        return "";
    }

    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void login(String username, String password) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        // no-op
    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0L;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public String[] getParameterValues(String name) {
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Map.of();
    }

    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public int getServerPort() {
        return 80;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteAddr() {
        return "127.0.0.1";
    }

    @Override
    public String getRemoteHost() {
        return "localhost";
    }

    @Override
    public void setAttribute(String name, Object o) {
        // no-op
    }

    @Override
    public void removeAttribute(String name) {
        // no-op
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(List.of(getLocale()));
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalName() {
        return getServerName();
    }

    @Override
    public String getLocalAddr() {
        return "127.0.0.1";
    }

    @Override
    public int getLocalPort() {
        return getServerPort();
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    @Override
    public ServletConnection getServletConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }
}
