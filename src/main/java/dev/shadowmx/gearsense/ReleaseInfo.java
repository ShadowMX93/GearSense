package dev.shadowmx.gearsense;

record ReleaseInfo(String version, String releaseUrl, String jarUrl) {
    boolean hasJar() {
        return jarUrl != null && !jarUrl.isBlank();
    }
}
