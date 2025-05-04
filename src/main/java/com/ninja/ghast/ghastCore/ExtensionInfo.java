package com.ninja.ghast.ghastCore;

public class ExtensionInfo {
    public final String name;
    public final String authors;
    public final String version;
    public final String namespace;

    public ExtensionInfo(String name, String authors, String version, String namespace) {
        this.name = name;
        this.authors = authors;
        this.version = version;
        this.namespace = namespace;
    }
}