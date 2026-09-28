package tr.org.tspb.util.tools;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Application;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility to scan and print registered JAX-RS REST API endpoints and deployment command,
 * matching the format used across FMS projects.
 *
 * @author Telman Şahbazoğlu
 */
public class ApiPathPrinter {

    public static class EndpointInfo implements Comparable<EndpointInfo> {
        private final String httpMethod;
        private final String fullUri;
        private final String apiPath;
        private final String consumes;
        private final String produces;
        private final String handler;

        public EndpointInfo(String httpMethod, String fullUri, String apiPath, String consumes, String produces, String handler) {
            this.httpMethod = httpMethod;
            this.fullUri = fullUri;
            this.apiPath = apiPath;
            this.consumes = consumes;
            this.produces = produces;
            this.handler = handler;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public String getFullUri() {
            return fullUri;
        }

        public String getApiPath() {
            return apiPath;
        }

        public String getConsumes() {
            return consumes;
        }

        public String getProduces() {
            return produces;
        }

        public String getHandler() {
            return handler;
        }

        @Override
        public int compareTo(EndpointInfo o) {
            int pathCmp = this.fullUri.compareTo(o.fullUri);
            if (pathCmp != 0) {
                return pathCmp;
            }
            return this.httpMethod.compareTo(o.httpMethod);
        }
    }

    public static void main(String[] args) {
        String deployCommand = null;
        if (args != null && args.length > 0) {
            deployCommand = String.join(" ", args);
        }
        if (deployCommand == null || deployCommand.isBlank()) {
            deployCommand = System.getProperty("deployCommand");
        }
        printApiPaths(deployCommand);
    }

    public static void printApiPaths() {
        printApiPaths(System.getProperty("deployCommand"));
    }

    public static void printApiPaths(String deployCommand) {
        String contextRoot = System.getProperty("contextRoot", "/");
        List<EndpointInfo> endpoints = scanEndpoints(contextRoot);

        if (!endpoints.isEmpty()) {
            int maxMethodLen = Math.max("METHOD".length(), endpoints.stream().mapToInt(e -> e.getHttpMethod().length()).max().orElse(6));
            int maxUriLen = Math.max("DEPLOYED URI".length(), endpoints.stream().mapToInt(e -> e.getFullUri().length()).max().orElse(20));
            int maxConsumesLen = Math.max("CONSUMES".length(), endpoints.stream().mapToInt(e -> e.getConsumes().length()).max().orElse(10));
            int maxProducesLen = Math.max("PRODUCES".length(), endpoints.stream().mapToInt(e -> e.getProduces().length()).max().orElse(10));
            int maxHandlerLen = Math.max("HANDLER".length(), endpoints.stream().mapToInt(e -> e.getHandler().length()).max().orElse(15));

            int totalWidth = maxMethodLen + maxUriLen + maxConsumesLen + maxProducesLen + maxHandlerLen + 12;
            String line = "=".repeat(Math.max(totalWidth, 80));
            String subLine = "-".repeat(Math.max(totalWidth, 80));

            System.out.println();
            System.out.println(line);
            System.out.println("  FMS - REGISTERED REST API ENDPOINTS");
            System.out.println("  Context Root: " + contextRoot + " | Application Path: " + detectApplicationPath());
            System.out.println(subLine);

            String format = "  %-" + maxMethodLen + "s  %-" + maxUriLen + "s  %-" + maxConsumesLen + "s  %-" + maxProducesLen + "s  %-" + maxHandlerLen + "s";
            System.out.println(String.format(format, "METHOD", "DEPLOYED URI", "CONSUMES", "PRODUCES", "HANDLER"));
            System.out.println(subLine);

            for (EndpointInfo ep : endpoints) {
                System.out.println(String.format(format,
                        ep.getHttpMethod(),
                        ep.getFullUri(),
                        ep.getConsumes(),
                        ep.getProduces(),
                        ep.getHandler()));
            }

            System.out.println(line);
            System.out.println("  Total Endpoints: " + endpoints.size());
            System.out.println(line);
            System.out.println();
        } else {
            System.out.println();
            System.out.println("[API Paths] No JAX-RS endpoints found.");
            System.out.println();
        }

        if (deployCommand != null && !deployCommand.isBlank()) {
            System.out.println(deployCommand);
            System.out.println();
        }
    }

    public static String detectApplicationPath() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ApiPathPrinter.class.getClassLoader();
        }
        List<Class<?>> allClasses = findAllClasses(cl);
        for (Class<?> clazz : allClasses) {
            if (clazz.isAnnotationPresent(ApplicationPath.class)) {
                return clazz.getAnnotation(ApplicationPath.class).value();
            }
        }
        return "/";
    }

    public static List<EndpointInfo> scanEndpoints(String contextRoot) {
        List<EndpointInfo> endpoints = new ArrayList<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ApiPathPrinter.class.getClassLoader();
        }

        List<Class<?>> allClasses = findAllClasses(cl);

        // 1. Find ApplicationPath
        String appBasePath = detectApplicationPath();

        // 2. Scan Resource classes
        for (Class<?> clazz : allClasses) {
            if (clazz.isAnnotationPresent(Path.class)) {
                String classPath = clazz.getAnnotation(Path.class).value();
                Consumes classConsumes = clazz.getAnnotation(Consumes.class);
                Produces classProduces = clazz.getAnnotation(Produces.class);

                for (Method method : clazz.getDeclaredMethods()) {
                    String httpVerb = getHttpVerb(method);
                    if (httpVerb == null) {
                        continue;
                    }

                    Path methodPathAnnotation = method.getAnnotation(Path.class);
                    String methodPath = methodPathAnnotation != null ? methodPathAnnotation.value() : "";

                    Consumes methodConsumes = method.getAnnotation(Consumes.class);
                    Produces methodProduces = method.getAnnotation(Produces.class);

                    String consumesStr;
                    if ("GET".equalsIgnoreCase(httpVerb) && methodConsumes == null) {
                        consumesStr = "-";
                    } else if (methodConsumes != null) {
                        consumesStr = formatMediaTypes(methodConsumes.value());
                    } else if (classConsumes != null) {
                        consumesStr = formatMediaTypes(classConsumes.value());
                    } else {
                        consumesStr = "-";
                    }

                    String producesStr;
                    if (methodProduces != null) {
                        producesStr = formatMediaTypes(methodProduces.value());
                    } else if (classProduces != null) {
                        producesStr = formatMediaTypes(classProduces.value());
                    } else {
                        producesStr = "-";
                    }

                    String apiPath = normalizePath(appBasePath, classPath, methodPath);
                    String fullUri = normalizePath(contextRoot, appBasePath, classPath, methodPath);
                    String handler = clazz.getSimpleName() + "#" + method.getName();

                    endpoints.add(new EndpointInfo(httpVerb, fullUri, apiPath, consumesStr, producesStr, handler));
                }
            }
        }

        Map<String, EndpointInfo> uniqueMap = new LinkedHashMap<>();
        for (EndpointInfo ep : endpoints) {
            String key = ep.getHttpMethod() + " " + ep.getFullUri();
            uniqueMap.putIfAbsent(key, ep);
        }

        List<EndpointInfo> result = new ArrayList<>(uniqueMap.values());
        Collections.sort(result);
        return result;
    }

    private static String getHttpVerb(Method method) {
        if (method.isAnnotationPresent(GET.class)) return "GET";
        if (method.isAnnotationPresent(POST.class)) return "POST";
        if (method.isAnnotationPresent(PUT.class)) return "PUT";
        if (method.isAnnotationPresent(DELETE.class)) return "DELETE";
        if (method.isAnnotationPresent(PATCH.class)) return "PATCH";
        if (method.isAnnotationPresent(HEAD.class)) return "HEAD";
        if (method.isAnnotationPresent(OPTIONS.class)) return "OPTIONS";

        for (Annotation ann : method.getAnnotations()) {
            HttpMethod httpMethod = ann.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethod != null) {
                return httpMethod.value();
            }
        }
        return null;
    }

    private static String formatMediaTypes(String[] mediaTypes) {
        if (mediaTypes == null || mediaTypes.length == 0) {
            return "-";
        }
        return String.join(",", mediaTypes);
    }

    public static String normalizePath(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String clean = part.trim();
            if (!clean.startsWith("/")) {
                clean = "/" + clean;
            }
            if (clean.endsWith("/") && clean.length() > 1) {
                clean = clean.substring(0, clean.length() - 1);
            }
            sb.append(clean);
        }
        String result = sb.toString().replaceAll("/{2,}", "/");
        return result.isEmpty() ? "/" : result;
    }

    private static List<Class<?>> findAllClasses(ClassLoader cl) {
        Set<Class<?>> classes = new HashSet<>();

        // Approach 1: scan directory from CodeSource
        try {
            URL codeSourceUrl = ApiPathPrinter.class.getProtectionDomain().getCodeSource().getLocation();
            if (codeSourceUrl != null && "file".equalsIgnoreCase(codeSourceUrl.getProtocol())) {
                File rootDir = new File(codeSourceUrl.toURI());
                if (rootDir.isDirectory()) {
                    scanClassesInDirectory(rootDir, rootDir, cl, classes);
                }
            }
        } catch (Throwable ignored) {
        }

        // Approach 2: scan class path entries (directories and jars belonging to the project)
        try {
            String classPath = System.getProperty("java.class.path");
            if (classPath != null) {
                String[] entries = classPath.split(File.pathSeparator);
                for (String entry : entries) {
                    File file = new File(entry);
                    if (file.isDirectory()) {
                        scanClassesInDirectory(file, file, cl, classes);
                    } else if (file.isFile() && file.getName().endsWith(".jar") && (file.getName().contains("fms-") || file.getName().contains("service"))) {
                        scanClassesInJar(file, cl, classes);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Approach 3: fallback to known classes
        String[] knownClassNames = {
            "tr.org.tspb.service.api.LmsRestGateway"
        };
        for (String name : knownClassNames) {
            try {
                classes.add(Class.forName(name, false, cl));
            } catch (Throwable ignored) {
            }
        }

        return new ArrayList<>(classes);
    }

    private static void scanClassesInJar(File jarFile, ClassLoader cl, Set<Class<?>> classes) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class") && !name.contains("$") && name.startsWith("tr/org/tspb/")) {
                    String className = name.replace('/', '.').substring(0, name.length() - 6);
                    try {
                        classes.add(Class.forName(className, false, cl));
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void scanClassesInDirectory(File currentDir, File rootDir, ClassLoader cl, Set<Class<?>> classes) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanClassesInDirectory(file, rootDir, cl, classes);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String rootPath = rootDir.getAbsolutePath();
                String filePath = file.getAbsolutePath();
                if (filePath.startsWith(rootPath)) {
                    String relative = filePath.substring(rootPath.length());
                    if (relative.startsWith(File.separator)) {
                        relative = relative.substring(1);
                    }
                    String className = relative
                            .replace(File.separatorChar, '.')
                            .replaceAll("\\.class$", "");
                    try {
                        classes.add(Class.forName(className, false, cl));
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }
}
